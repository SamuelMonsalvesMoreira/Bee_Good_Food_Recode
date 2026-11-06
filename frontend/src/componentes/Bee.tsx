// Mascote Bee: abelha simples, legivel em 24px e em tamanho de ilustracao. Decorativa por padrao.

interface Props {
  tamanho?: number;
  rotulo?: string;
  humor?: 'feliz' | 'pensativa';
}

export function Bee({ tamanho = 40, rotulo, humor = 'feliz' }: Props) {
  return (
    <svg
      width={tamanho}
      height={tamanho}
      viewBox="0 0 64 64"
      role={rotulo ? 'img' : undefined}
      aria-label={rotulo}
      aria-hidden={rotulo ? undefined : true}
      focusable="false"
    >
      {/* Asas */}
      <ellipse cx="25" cy="17" rx="10" ry="13" transform="rotate(-25 25 17)" fill="#dff1f7" stroke="#2b1d0e" strokeWidth="2.5" />
      <ellipse cx="40" cy="15" rx="9" ry="12" transform="rotate(20 40 15)" fill="#dff1f7" stroke="#2b1d0e" strokeWidth="2.5" />
      {/* Corpo */}
      <ellipse cx="32" cy="38" rx="21" ry="17" fill="#f2b705" stroke="#2b1d0e" strokeWidth="2.5" />
      <path d="M27 22.5c-3 9-3 22 0 31" stroke="#2b1d0e" strokeWidth="5" fill="none" strokeLinecap="round" />
      <path d="M38 22.5c3 9 3 22 0 31" stroke="#2b1d0e" strokeWidth="5" fill="none" strokeLinecap="round" />
      {/* Ferrao */}
      <path d="M53 38l6 0" stroke="#2b1d0e" strokeWidth="2.5" strokeLinecap="round" />
      {/* Rosto */}
      <circle cx="17" cy="35" r="2.6" fill="#2b1d0e" />
      {humor === 'feliz'
        ? <path d="M14 42c2.5 2.6 6 2.6 8 0" stroke="#2b1d0e" strokeWidth="2.2" fill="none" strokeLinecap="round" />
        : <path d="M14.5 43h7" stroke="#2b1d0e" strokeWidth="2.2" strokeLinecap="round" />}
      {/* Antenas */}
      <path d="M15 25c-2-5-5-7-8-7" stroke="#2b1d0e" strokeWidth="2.2" fill="none" strokeLinecap="round" />
      <circle cx="6.5" cy="18" r="2.2" fill="#2b1d0e" />
    </svg>
  );
}
