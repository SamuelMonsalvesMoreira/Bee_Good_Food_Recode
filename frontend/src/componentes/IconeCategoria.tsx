import { Apple, CakeSlice, Croissant, Package, Salad, Soup, Coffee, type LucideIcon } from 'lucide-react';

// Os produtos nao tem foto no MVP: cada categoria ganha um icone e um tom proprio para dar identidade ao cartao.

interface Visual { icone: LucideIcon; tom: 'mel' | 'folha' | 'rosa' | 'brasa' | 'neutro' }

const POR_PALAVRA: [RegExp, Visual][] = [
  [/pad|pao|paes/, { icone: Croissant, tom: 'mel' }],
  [/confeit|doce|bolo|torta/, { icone: CakeSlice, tom: 'rosa' }],
  [/horti|fruta|verdura|feira/, { icone: Apple, tom: 'folha' }],
  [/salada|natural/, { icone: Salad, tom: 'folha' }],
  [/refei|marmita|prato|almoco/, { icone: Soup, tom: 'brasa' }],
  [/cafe|bebida/, { icone: Coffee, tom: 'mel' }],
];

export function visualDaCategoria(slug: string): Visual {
  return POR_PALAVRA.find(([padrao]) => padrao.test(slug))?.[1] ?? { icone: Package, tom: 'neutro' };
}

export function IconeCategoria({ slug, tamanho = 'medio' }: { slug: string; tamanho?: 'medio' | 'grande' }) {
  const { icone: Icone, tom } = visualDaCategoria(slug);
  return (
    <span className={`icone-categoria icone-${tom} icone-${tamanho}`} aria-hidden="true">
      <Icone strokeWidth={1.8} />
    </span>
  );
}
