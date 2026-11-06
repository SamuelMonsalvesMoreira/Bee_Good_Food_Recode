import type { Pagina } from '../api/types';

export function Paginacao({ pagina, aoMudar }: { pagina: Pagina<unknown>; aoMudar: (pagina: number) => void }) {
  if (pagina.totalPaginas <= 1) return null;
  return (
    <nav className="paginacao" aria-label="Paginação">
      <button className="botao botao-secundario botao-pequeno" disabled={pagina.pagina === 0} onClick={() => aoMudar(pagina.pagina - 1)}>
        Anterior
      </button>
      <span className="numero">Página {pagina.pagina + 1} de {pagina.totalPaginas}</span>
      <button className="botao botao-secundario botao-pequeno" disabled={pagina.ultima} onClick={() => aoMudar(pagina.pagina + 1)}>
        Próxima
      </button>
    </nav>
  );
}
