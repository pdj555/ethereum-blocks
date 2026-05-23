"use client";

type SectionFrameProps = {
  title: string;
  children: React.ReactNode;
  className?: string;
};

export function SectionFrame({ title, children, className = "" }: SectionFrameProps) {
  return (
    <section className={`section-frame ${className}`.trim()}>
      <h2 className="section-frame__title">{title}</h2>
      {children}
    </section>
  );
}
