# Chamado por database/scripts/validar-banco.ps1 -Api com MySQL isolado ja iniciado.
[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$JdbcUrl,
    [Parameter(Mandatory)][string]$DbUser,
    [Parameter(Mandatory)][string]$DbPassword,
    [string]$JavaHome = $env:JAVA_HOME,
    [string]$MavenCommand,
    [string]$MavenRepository
)
$ErrorActionPreference = 'Stop'
$backendDir = Split-Path $PSScriptRoot -Parent
if ($JdbcUrl -notmatch '^jdbc:mysql://127\.0\.0\.1:[0-9]+/bee_good_food_validacao\?') {
    throw 'Use somente a instancia isolada criada pelo runner do banco.'
}
if (-not $JavaHome) { throw 'Defina JAVA_HOME com um JDK 21 ou superior.' }
$java = Join-Path $JavaHome 'bin\java.exe'
if (-not (Test-Path -LiteralPath $java)) { throw 'JAVA_HOME nao aponta para um JDK valido.' }
if (-not $MavenCommand) { $MavenCommand = Join-Path $backendDir 'mvnw.cmd' }
if (-not (Test-Path -LiteralPath $MavenCommand)) { throw 'MavenCommand deve apontar para mvn.cmd ou mvnw.cmd.' }
$outputDir = Join-Path $backendDir 'target'
$evidenceDir = Join-Path $backendDir 'evidencias'
New-Item -ItemType Directory -Force -Path $outputDir, $evidenceDir | Out-Null
$utf8 = [Text.UTF8Encoding]::new($false)
$startedAt = [DateTime]::UtcNow
# Chave exclusiva desta execucao; nao aparece na linha de comando nem nas evidencias.
$jwtBytes = [byte[]]::new(32)
$jwtRandom = [Security.Cryptography.RandomNumberGenerator]::Create()
try { $jwtRandom.GetBytes($jwtBytes) } finally { $jwtRandom.Dispose() }
$jwtSecret = [Convert]::ToBase64String($jwtBytes)

function Start-ApiProcess {
    param([string]$Executable, [string]$Arguments)
    $info = [Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $Executable
    $info.Arguments = $Arguments
    $info.WorkingDirectory = $backendDir
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardOutputEncoding = $utf8
    $info.StandardErrorEncoding = $utf8
    $info.EnvironmentVariables['JAVA_HOME'] = $JavaHome
    $info.EnvironmentVariables['DB_URL'] = $JdbcUrl
    $info.EnvironmentVariables['DB_USER'] = $DbUser
    $info.EnvironmentVariables['DB_PASSWORD'] = $DbPassword
    $info.EnvironmentVariables['BEE_TEST_MYSQL_URL'] = $JdbcUrl
    $info.EnvironmentVariables['BEE_TEST_MYSQL_USER'] = $DbUser
    $info.EnvironmentVariables['BEE_TEST_MYSQL_PASSWORD'] = $DbPassword
    $info.EnvironmentVariables['JWT_SECRET_BASE64'] = $jwtSecret
    $process = [Diagnostics.Process]::Start($info)
    return @{ Process=$process; Output=$process.StandardOutput.ReadToEndAsync(); Error=$process.StandardError.ReadToEndAsync() }
}

function Save-ProcessLog {
    param($ProcessInfo, [string]$Name)
    $text = $ProcessInfo.Output.GetAwaiter().GetResult() + "`n" + $ProcessInfo.Error.GetAwaiter().GetResult()
    [IO.File]::WriteAllText((Join-Path $outputDir $Name), $text, $utf8)
    return $text
}

try {
    $argsText = "/d /s /c `"`"$MavenCommand`" --batch-mode --no-transfer-progress -Pintegration clean verify"
    if ($MavenRepository) { $argsText += " `"-Dmaven.repo.local=$MavenRepository`"" }
    $argsText += '"'
    Write-Host 'Compilando e executando testes HTTP com MySQL real...'
    $build = Start-ApiProcess -Executable $env:ComSpec -Arguments $argsText
    $clock = [Diagnostics.Stopwatch]::StartNew()
    while (-not $build.Process.WaitForExit(1000)) {
        if ($clock.Elapsed.TotalSeconds -gt 300) {
            $build.Process.Kill($true)
            throw 'Build excedeu cinco minutos. Verifique a conexao e as dependencias Maven.'
        }
        if ([int]$clock.Elapsed.TotalSeconds % 20 -eq 0) { Write-Host 'Maven ainda esta compilando ou executando os testes...' }
    }
    $log = Save-ProcessLog $build 'validacao-api-build.log'
    if ($build.Process.ExitCode -ne 0) {
        Write-Host ($log -split '\r?\n' | Select-Object -Last 45 | Out-String)
        throw 'Build/testes falharam. Consulte backend/target/validacao-api-build.log.'
    }
    $reports = @(Get-ChildItem -LiteralPath (Join-Path $outputDir 'failsafe-reports') -Filter 'TEST-*.xml')
    if ($reports.Count -eq 0) { throw 'Nenhum teste de integracao foi executado.' }
    $total = 0
    foreach ($file in $reports) {
        [xml]$xml = [IO.File]::ReadAllText($file.FullName)
        $suite = $xml.testsuite
        if ([int]$suite.failures -gt 0 -or [int]$suite.errors -gt 0 -or [int]$suite.skipped -gt 0) { throw 'Existem falhas ou testes ignorados.' }
        $total += [int]$suite.tests
    }
    $unitTests = 0
    foreach ($file in @(Get-ChildItem -LiteralPath (Join-Path $outputDir 'surefire-reports') -Filter 'TEST-*.xml')) {
        [xml]$xml = [IO.File]::ReadAllText($file.FullName)
        if ([int]$xml.testsuite.failures -gt 0 -or [int]$xml.testsuite.errors -gt 0 -or [int]$xml.testsuite.skipped -gt 0) { throw 'Falhas em testes unitarios.' }
        $unitTests += [int]$xml.testsuite.tests
    }

    $jar = Join-Path $outputDir 'bee-good-food-api-0.1.0-SNAPSHOT.jar'
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $archive = [IO.Compression.ZipFile]::OpenRead($jar)
    try {
        $migrationFiles = @($archive.Entries | Where-Object { $_.FullName -match '^BOOT-INF/classes/db/migration/V.*\.sql$' })
        if ($migrationFiles.Count -ne 2) { throw 'O JAR deve conter exatamente V1 e V2.' }
        if (@($archive.Entries | Where-Object { $_.FullName -match '^BOOT-INF/classes/.*(demonstracao\.sql|integridade\.sql|ApiMySqlIT)' }).Count) { throw 'Dados/testes nao podem entrar no JAR.' }
    } finally { $archive.Dispose() }

    # Testar o JAR pronto duas vezes no mesmo banco: migrations nao podem ser reaplicadas.
    $restarts = @()
    $jarEmail = 'jar-' + [guid]::NewGuid().ToString('N') + '@example.invalid'
    $jarPassword = [guid]::NewGuid().ToString('N')
    $previousToken = $null
    for ($run=1; $run -le 2; $run++) {
        $listener = [Net.Sockets.TcpListener]::new([Net.IPAddress]::Loopback, 0)
        $listener.Start(); $port = $listener.LocalEndpoint.Port; $listener.Stop()
        $app = Start-ApiProcess -Executable $java -Arguments "-jar `"$jar`" --server.address=127.0.0.1 --server.port=$port"
        try {
            $ready = $false
            for ($attempt=0; $attempt -lt 90; $attempt++) {
                if ($app.Process.HasExited) { throw 'JAR encerrou antes de ficar pronto.' }
                try {
                    $health = Invoke-RestMethod -Uri "http://127.0.0.1:$port/actuator/health" -TimeoutSec 2
                    if ($health.status -eq 'UP') { $ready=$true; break }
                } catch { Start-Sleep -Milliseconds 500 }
            }
            if (-not $ready) { throw 'API nao ficou pronta no prazo.' }
            $categories = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/v1/categorias" -TimeoutSec 5
            if ($categories.totalElementos -ne 2) { throw 'Categorias nao preservadas na reinicializacao.' }
            if ($run -eq 1) {
                $signupBody = @{nome='Cliente de teste do JAR'; email=$jarEmail; senha=$jarPassword; maiorDeIdade=$true} | ConvertTo-Json
                $created = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/v1/auth/cadastro" -Method Post -ContentType 'application/json' -Body $signupBody -TimeoutSec 10
                if ($created.papeis.Count -ne 1 -or $created.papeis[0] -ne 'CLIENTE') { throw 'Cadastro do JAR nao restringiu o papel.' }
            } else {
                $persisted = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/v1/auth/me" -Headers @{Authorization="Bearer $previousToken"} -TimeoutSec 5
                if ($persisted.email -ne $jarEmail) { throw 'Sessao nao preservada com a mesma chave apos reinicio.' }
            }
            $loginBody = @{email=$jarEmail; senha=$jarPassword} | ConvertTo-Json
            $session = Invoke-RestMethod -Uri "http://127.0.0.1:$port/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body $loginBody -TimeoutSec 10
            if ($session.expiraEmSegundos -ne 900) { throw 'Validade inesperada do token.' }
            $previousToken = $session.accessToken
            if ($run -eq 1) {
                # Duas chamadas ja feitas (cadastro/login). Validar o limite padrao 20 via HTTP real.
                for ($attempt=0; $attempt -lt 18; $attempt++) {
                    $invalid = Invoke-WebRequest -Uri "http://127.0.0.1:$port/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body '{}' -SkipHttpErrorCheck -TimeoutSec 5
                    if ($invalid.StatusCode -ne 400) { throw 'Limite foi atingido antes das 20 tentativas.' }
                }
                $limited = Invoke-WebRequest -Uri "http://127.0.0.1:$port/api/v1/auth/login" -Method Post -ContentType 'application/json' -Body '{}' -SkipHttpErrorCheck -TimeoutSec 5
                if ($limited.StatusCode -ne 429 -or $limited.Headers['Retry-After'][0] -ne '60') { throw 'Limite de tentativas nao foi aplicado pelo JAR.' }
                Write-Host 'PASS | JAR: cadastro, login e limite de 20 tentativas por minuto'
            }
            if ($run -eq 1) {
                $openapi = Invoke-RestMethod -Uri "http://127.0.0.1:$port/v3/api-docs" -TimeoutSec 10
                # Remove o endereco efemero do servidor antes de registrar o contrato.
                $openapi.PSObject.Properties.Remove('servers')
                [IO.File]::WriteAllText((Join-Path $evidenceDir 'openapi.json'), ($openapi | ConvertTo-Json -Depth 40) + "`n", $utf8)
            }
            $restarts += "Inicializacao $run do JAR: health UP, categorias preservadas e login funcional"
            Write-Host "PASS | $($restarts[-1])"
        } finally {
            if (-not $app.Process.HasExited) { $app.Process.Kill(); $app.Process.WaitForExit() }
            $null = Save-ProcessLog $app "validacao-api-jar-$run.log"
        }
    }
    $report = [ordered]@{
        status='PASS'; inicioUtc=$startedAt.ToString('o'); fimUtc=[DateTime]::UtcNow.ToString('o')
        springBoot='4.1.1'; springdoc='3.1.1'; javaRelease=21; testesHttpMySql=$total; testesUnitarios=$unitTests
        migrations=@('V1__criar_schema_inicial.sql','V2__cadastrar_papeis.sql')
        empacotamento='Somente migrations no JAR; sem seeds ou testes'
        reinicializacoes=$restarts
        segurancaJar=@('Cadastro restrito a CLIENTE', 'JWT de 900 segundos', 'Conta e sessao persistidas entre reinicios com mesma chave', 'HTTP 429 na tentativa 21 com Retry-After')
        limites=@('Catalogo somente de leitura; cadastro/login e consulta de perfil implementados.', 'Pedidos e autorizacao por estabelecimento ainda nao implementados.', 'Limite de tentativas local; nao valida deploy publico ou multiplas instancias.')
    }
    [IO.File]::WriteAllText((Join-Path $evidenceDir 'ultima-validacao.json'), ($report | ConvertTo-Json -Depth 6) + "`n", $utf8)
    Write-Host "API VALIDADA: $total testes HTTP/MySQL, empacotamento e duas inicializacoes do JAR."
} catch {
    $report = @{status='FAIL'; instanteUtc=[DateTime]::UtcNow.ToString('o'); erro=$_.Exception.Message}
    [IO.File]::WriteAllText((Join-Path $evidenceDir 'ultima-validacao.json'), ($report | ConvertTo-Json), $utf8)
    throw
}
