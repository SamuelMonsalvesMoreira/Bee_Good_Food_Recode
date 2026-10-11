# Executa MySQL instalado em uma instancia isolada, sem usar o servico ou bancos existentes.
# PowerShell 5.1+; nao requer Docker, senha do MySQL existente ou privilegio de administrador.
[CmdletBinding()]
param(
    [string]$MySqlBin = 'C:\Program Files\MySQL\MySQL Server 8.0\bin'
)

$ErrorActionPreference = 'Stop'
$databaseDir = Split-Path $PSScriptRoot -Parent
$repoDir = Split-Path $databaseDir -Parent
$mysql = Join-Path $MySqlBin 'mysql.exe'
$mysqld = Join-Path $MySqlBin 'mysqld.exe'
foreach ($binary in @($mysql, $mysqld)) {
    if (-not (Test-Path -LiteralPath $binary)) { throw "Executavel nao encontrado: $binary. Informe -MySqlBin." }
}

$runId = [guid]::NewGuid().ToString('N')
$runDir = Join-Path $repoDir ".local-mysql\$runId"
$dataDir = Join-Path $runDir 'data'
$evidenceDir = Join-Path $databaseDir 'evidencias'
New-Item -ItemType Directory -Path $runDir -Force | Out-Null
New-Item -ItemType Directory -Path $evidenceDir -Force | Out-Null

# Porta livre escolhida pelo Windows, sempre ligada somente ao loopback.
$listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
$listener.Start()
$port = $listener.LocalEndpoint.Port
$listener.Stop()
$script:dbPassword = ''
$server = $null
$allTests = [System.Collections.Generic.List[string]]::new()
$utf8 = [System.Text.UTF8Encoding]::new($false)
$startedAt = [DateTime]::UtcNow

function Start-Client {
    param([string]$Sql, [switch]$WithoutDatabase)
    $info = [System.Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $mysql
    $info.Arguments = "--no-defaults --protocol=TCP --host=127.0.0.1 --port=$port --user=root --connect-timeout=2 --default-character-set=utf8mb4 --batch --skip-column-names"
    if (-not $WithoutDatabase) { $info.Arguments += ' --database=bee_good_food_validacao' }
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardInput = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $info.StandardInputEncoding = $utf8
    $info.StandardOutputEncoding = $utf8
    $info.StandardErrorEncoding = $utf8
    # Senha aleatoria somente no ambiente do processo filho; nunca na linha de comando.
    $info.EnvironmentVariables['MYSQL_PWD'] = $script:dbPassword
    $process = [System.Diagnostics.Process]::Start($info)
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.WriteLine($Sql)
    $process.StandardInput.Close()
    return @{ Process = $process; Output = $stdout; Error = $stderr }
}

function Complete-Client {
    param($Client)
    if (-not $Client.Process.WaitForExit(30000)) {
        $Client.Process.Kill()
        throw 'Consulta excedeu 30 segundos.'
    }
    $outputText = $Client.Output.GetAwaiter().GetResult()
    $errorText = $Client.Error.GetAwaiter().GetResult()
    if ($Client.Process.ExitCode -ne 0) { throw "MySQL recusou a consulta: $errorText" }
    return $outputText.Trim()
}

function Invoke-Sql {
    param([string]$Sql, [switch]$WithoutDatabase)
    Complete-Client (Start-Client -Sql $Sql -WithoutDatabase:$WithoutDatabase)
}

function Assert-Result {
    param([bool]$Passed, [string]$Name)
    if (-not $Passed) { throw "FAIL | $Name" }
    $allTests.Add($Name)
    Write-Host "PASS | $Name"
}

function Get-MigrationHash {
    param([string]$Path)
    # Git pode converter LF em CRLF no Windows; comparar o mesmo texto SQL.
    $text = [IO.File]::ReadAllText($Path).Replace("`r`n", "`n").Replace("`r", "`n")
    $sha = [Security.Cryptography.SHA256]::Create()
    try { return [BitConverter]::ToString($sha.ComputeHash($utf8.GetBytes($text))).Replace('-', '') }
    finally { $sha.Dispose() }
}

try {
    Write-Host 'Inicializando instancia MySQL isolada...'
    $initialize = Start-Process -FilePath $mysqld -ArgumentList @('--no-defaults', '--initialize-insecure', "--datadir=`"$dataDir`"") -WindowStyle Hidden -PassThru
    if (-not $initialize.WaitForExit(60000)) { $initialize.Kill(); throw 'Inicializacao excedeu 60 segundos.' }
    if ($initialize.ExitCode -ne 0) { throw "Falha ao inicializar MySQL. Consulte os logs em $runDir" }

    $server = Start-Process -FilePath $mysqld -ArgumentList @(
        '--no-defaults', "--datadir=`"$dataDir`"", '--bind-address=127.0.0.1', "--port=$port",
        '--mysqlx=OFF', '--default-time-zone=+00:00', '--local-infile=OFF',
        '--sql-mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_ENGINE_SUBSTITUTION',
        "--log-error=`"$(Join-Path $runDir 'server.log')`""
    ) -WindowStyle Hidden -PassThru

    $ready = $false
    $lastConnectionError = ''
    for ($attempt = 0; $attempt -lt 60; $attempt++) {
        if ($server.HasExited) { throw "Servidor encerrou inesperadamente. Consulte $runDir\server.log" }
        try { $null = Invoke-Sql -Sql 'SELECT 1;' -WithoutDatabase; $ready = $true; break } catch { $lastConnectionError = $_.Exception.Message; Start-Sleep -Milliseconds 250 }
    }
    if (-not $ready) { throw "Conexao MySQL indisponivel: $lastConnectionError" }

    $newPassword = [guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')
    $null = Invoke-Sql -Sql "ALTER USER 'root'@'localhost' IDENTIFIED BY '$newPassword';" -WithoutDatabase
    $script:dbPassword = $newPassword
    $version = Invoke-Sql -Sql 'SELECT VERSION();' -WithoutDatabase
    $null = Invoke-Sql -Sql 'CREATE DATABASE bee_good_food_validacao CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;' -WithoutDatabase
    $migrationEvidence = @()
    # Lista explicita: acompanha a ordem do Flyway, sem ordenar V10 antes de V2.
    foreach ($migration in @('V1__criar_schema_inicial.sql', 'V2__cadastrar_papeis.sql')) {
        $migrationPath = Join-Path $databaseDir "migrations\$migration"
        $null = Invoke-Sql ([IO.File]::ReadAllText($migrationPath))
        $migrationEvidence += @{ arquivo = $migration; sha256 = (Get-MigrationHash $migrationPath) }
        Assert-Result $true "Migration $migration executada no MySQL real"
    }
    $null = Invoke-Sql ([IO.File]::ReadAllText((Join-Path $databaseDir 'seeds\demonstracao.sql')))
    $testOutput = Invoke-Sql ([IO.File]::ReadAllText((Join-Path $databaseDir 'tests\integridade.sql')))
    foreach ($line in ($testOutput -split '\r?\n')) {
        if ($line.StartsWith('PASS | ')) { $allTests.Add($line.Substring(7)); Write-Host $line }
    }

    # Duas conexoes disputam a ultima unidade. Testa o mecanismo SQL, nao a futura API.
    $null = Invoke-Sql 'UPDATE oferta SET estoque_disponivel=1 WHERE id=1;'
    $first = Start-Client "START TRANSACTION; UPDATE oferta SET estoque_disponivel=estoque_disponivel-1 WHERE id=1 AND estoque_disponivel>0; SELECT ROW_COUNT(); DO GET_LOCK('bee_teste_primeira_atualizou',0); DO SLEEP(2); COMMIT; DO RELEASE_LOCK('bee_teste_primeira_atualizou');"
    $locked = $false
    for ($attempt = 0; $attempt -lt 50; $attempt++) {
        if ((Invoke-Sql "SELECT IS_USED_LOCK('bee_teste_primeira_atualizou') IS NOT NULL;") -eq '1') { $locked = $true; break }
        Start-Sleep -Milliseconds 20
    }
    if (-not $locked) { throw 'Primeira conexao nao confirmou aquisicao do bloqueio.' }
    $second = Start-Client 'START TRANSACTION; UPDATE oferta SET estoque_disponivel=estoque_disponivel-1 WHERE id=1 AND estoque_disponivel>0; SELECT ROW_COUNT(); COMMIT;'
    $firstRows = Complete-Client $first
    $secondRows = Complete-Client $second
    $stock = Invoke-Sql 'SELECT estoque_disponivel FROM oferta WHERE id=1;'
    Assert-Result ($firstRows -eq '1' -and $secondRows -eq '0' -and $stock -eq '0') 'Concorrencia SQL: uma unica venda da ultima unidade em duas conexoes'
    $null = Invoke-Sql 'UPDATE oferta SET estoque_disponivel=8 WHERE id=1;'
    Assert-Result ((Invoke-Sql 'SELECT COUNT(*) FROM pedido;') -eq '1') 'Testes nao deixaram pedidos adicionais'
    Assert-Result ((Invoke-Sql 'SELECT nome FROM produto WHERE id=1;') -eq 'Marmita Demo') 'Testes preservaram dados de demonstracao'

    # Executar tambem as consultas didaticas garante que seus exemplos sao validos.
    $null = Invoke-Sql ([IO.File]::ReadAllText((Join-Path $databaseDir 'queries\consultas-estudo.sql')))
    Assert-Result $true 'Consultas de catalogo, historico, indicadores e auditoria executadas'

    $schema = Invoke-Sql "SELECT TABLE_NAME,COLUMN_NAME,COLUMN_TYPE,IS_NULLABLE,COALESCE(COLUMN_DEFAULT,'NULL'),COLUMN_KEY,EXTRA FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,ORDINAL_POSITION;"
    [IO.File]::WriteAllText((Join-Path $evidenceDir 'dicionario-fisico.tsv'), "tabela`tcoluna`ttipo`tnulo`tpadrao`tchave`textra`n$schema`n", $utf8)
    $foreignKeys = Invoke-Sql "SELECT TABLE_NAME,CONSTRAINT_NAME,COLUMN_NAME,REFERENCED_TABLE_NAME,REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND REFERENCED_TABLE_NAME IS NOT NULL ORDER BY TABLE_NAME,CONSTRAINT_NAME;"
    [IO.File]::WriteAllText((Join-Path $evidenceDir 'relacionamentos.tsv'), "tabela`tchave_estrangeira`tcoluna`ttabela_referenciada`tcoluna_referenciada`n$foreignKeys`n", $utf8)
    $indexes = Invoke-Sql "SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,SEQ_IN_INDEX,COLUMN_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;"
    [IO.File]::WriteAllText((Join-Path $evidenceDir 'indices.tsv'), "tabela`tindice`tnao_unico`tordem`tcoluna`n$indexes`n", $utf8)
    $report = [ordered]@{
        status = 'PASS'; inicioUtc = $startedAt.ToString('o'); fimUtc = [DateTime]::UtcNow.ToString('o')
        mysql = $version; instancia = 'Isolada, loopback, encerrada ao terminar'; schema = 'bee_good_food_validacao'
        migrations = $migrationEvidence
        normalizacaoHash = 'UTF-8 sem BOM, quebras de linha normalizadas para LF'
        totalTestes = $allTests.Count; testes = @($allTests.ToArray())
        limites = @('Nao executa Flyway: migration aplicada diretamente pelo cliente MySQL.', 'Nao valida API, autenticacao, HTTP ou interface.', 'Concorrencia validada em SQL; transacoes completas de pedidos e cancelamento serao testadas na API.')
    }
    [IO.File]::WriteAllText((Join-Path $evidenceDir 'ultima-validacao.json'), ($report | ConvertTo-Json -Depth 6) + "`n", $utf8)
    Write-Host "VALIDADO: $($allTests.Count) verificacoes em MySQL $version. Evidencias: $evidenceDir"
} catch {
    $failure = [ordered]@{ status = 'FAIL'; instanteUtc = [DateTime]::UtcNow.ToString('o'); erro = $_.Exception.Message; testesAprovados = @($allTests.ToArray()) }
    [IO.File]::WriteAllText((Join-Path $runDir 'falha.json'), ($failure | ConvertTo-Json -Depth 6), $utf8)
    [IO.File]::WriteAllText((Join-Path $evidenceDir 'ultima-validacao.json'), ($failure | ConvertTo-Json -Depth 6), $utf8)
    throw
} finally {
    if ($null -ne $server -and -not $server.HasExited) {
        try { $null = Invoke-Sql -Sql 'SHUTDOWN;' -WithoutDatabase } catch { Write-Warning 'Encerrando somente o processo MySQL iniciado por este script.' }
        if (-not $server.WaitForExit(15000)) { $server.Kill(); $server.WaitForExit() }
    }
    $script:dbPassword = ''
    Write-Host "Dados de teste preservados localmente em $runDir (ignorados pelo Git)."
}
