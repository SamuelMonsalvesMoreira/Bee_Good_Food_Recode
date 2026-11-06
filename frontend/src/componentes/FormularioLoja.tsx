import { useState, type FormEvent } from 'react';
import type { ApiError } from '../api/client';
import type { EstabelecimentoForm } from '../api/endpoints';
import type { Estabelecimento } from '../api/types';
import { Campo, CampoSelecao, CampoTexto, erroDoCampo } from './Campo';
import { ErroEnvio } from './Estados';

const UFS = ['AC', 'AL', 'AP', 'AM', 'BA', 'CE', 'DF', 'ES', 'GO', 'MA', 'MT', 'MS', 'MG', 'PA', 'PB', 'PR', 'PE', 'PI', 'RJ',
  'RN', 'RS', 'RO', 'RR', 'SC', 'SP', 'SE', 'TO'];

// Fusos IANA usados no Brasil. A API valida qualquer identificador de regiao.
const FUSOS = [
  ['America/Sao_Paulo', 'Brasília (Sudeste, Sul, Nordeste, GO, DF)'],
  ['America/Manaus', 'Amazonas (Manaus)'],
  ['America/Cuiaba', 'Mato Grosso'],
  ['America/Campo_Grande', 'Mato Grosso do Sul'],
  ['America/Porto_Velho', 'Rondônia'],
  ['America/Boa_Vista', 'Roraima'],
  ['America/Rio_Branco', 'Acre'],
  ['America/Belem', 'Pará e Amapá'],
  ['America/Fortaleza', 'Ceará, RN, PB, PI, MA'],
  ['America/Recife', 'Pernambuco'],
  ['America/Maceio', 'Alagoas e Sergipe'],
  ['America/Bahia', 'Bahia'],
  ['America/Araguaina', 'Tocantins'],
  ['America/Noronha', 'Fernando de Noronha'],
];

interface Props {
  inicial?: Estabelecimento;
  enviando: boolean;
  erro?: ApiError;
  rotuloEnviar: string;
  aoEnviar: (dados: EstabelecimentoForm) => void;
}

export function FormularioLoja({ inicial, enviando, erro, rotuloEnviar, aoEnviar }: Props) {
  const [dados, setDados] = useState<EstabelecimentoForm>({
    nome: inicial?.nome ?? '',
    descricao: inicial?.descricao ?? '',
    endereco: {
      logradouro: inicial?.endereco.logradouro ?? '',
      numero: inicial?.endereco.numero ?? '',
      complemento: inicial?.endereco.complemento ?? '',
      bairro: inicial?.endereco.bairro ?? '',
      cidade: inicial?.endereco.cidade ?? '',
      estado: inicial?.endereco.estado ?? 'SP',
      cep: inicial?.endereco.cep ?? '',
      fusoIana: inicial?.endereco.fusoIana ?? 'America/Sao_Paulo',
    },
  });
  const e = dados.endereco;
  const mudarEndereco = (campo: keyof typeof e, valor: string) => setDados({ ...dados, endereco: { ...e, [campo]: valor } });
  const erroEndereco = (campo: string) => erroDoCampo(erro, `endereco.${campo}`);

  function enviar(evento: FormEvent) {
    evento.preventDefault();
    aoEnviar(dados);
  }

  return (
    <form onSubmit={enviar} noValidate className="formulario-largo">
      <ErroEnvio erro={erro} />
      <fieldset>
        <legend>Sobre a loja</legend>
        <Campo rotulo="Nome da loja" required maxLength={160} value={dados.nome}
          onChange={(ev) => setDados({ ...dados, nome: ev.target.value })} erro={erroDoCampo(erro, 'nome')} />
        <CampoTexto rotulo="Descrição (opcional)" maxLength={500} value={dados.descricao}
          ajuda="Conte o que costuma sobrar no fim do dia: pães, bolos, frutas…"
          onChange={(ev) => setDados({ ...dados, descricao: ev.target.value })} erro={erroDoCampo(erro, 'descricao')} />
      </fieldset>
      <fieldset>
        <legend>Endereço de retirada</legend>
        <div className="grade-campos">
          <Campo rotulo="Rua" required maxLength={180} autoComplete="address-line1" value={e.logradouro}
            onChange={(ev) => mudarEndereco('logradouro', ev.target.value)} erro={erroEndereco('logradouro')} />
          <Campo rotulo="Número" required maxLength={30} value={e.numero}
            onChange={(ev) => mudarEndereco('numero', ev.target.value)} erro={erroEndereco('numero')} />
          <Campo rotulo="Complemento (opcional)" maxLength={120} value={e.complemento}
            onChange={(ev) => mudarEndereco('complemento', ev.target.value)} erro={erroEndereco('complemento')} />
          <Campo rotulo="Bairro" required maxLength={100} value={e.bairro}
            onChange={(ev) => mudarEndereco('bairro', ev.target.value)} erro={erroEndereco('bairro')} />
          <Campo rotulo="Cidade" required maxLength={100} autoComplete="address-level2" value={e.cidade}
            onChange={(ev) => mudarEndereco('cidade', ev.target.value)} erro={erroEndereco('cidade')} />
          <CampoSelecao rotulo="UF" value={e.estado} onChange={(ev) => mudarEndereco('estado', ev.target.value)} erro={erroEndereco('estado')}>
            {UFS.map((uf) => <option key={uf}>{uf}</option>)}
          </CampoSelecao>
          <Campo rotulo="CEP" required inputMode="numeric" autoComplete="postal-code" placeholder="00000-000" maxLength={9} value={e.cep}
            onChange={(ev) => mudarEndereco('cep', ev.target.value)} erro={erroEndereco('cep')} />
          <CampoSelecao rotulo="Fuso horário" value={e.fusoIana} onChange={(ev) => mudarEndereco('fusoIana', ev.target.value)}
            erro={erroEndereco('fusoIana')} ajuda="Os prazos das ofertas são mostrados neste horário.">
            {FUSOS.map(([valor, rotulo]) => <option key={valor} value={valor}>{rotulo}</option>)}
          </CampoSelecao>
        </div>
      </fieldset>
      <button className="botao botao-principal" disabled={enviando}>{enviando ? 'Enviando…' : rotuloEnviar}</button>
    </form>
  );
}
