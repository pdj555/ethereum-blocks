"use client";

type BootLoaderProps = {
  visible: boolean;
  stage: string;
  progress: number;
};

export function BootLoader({ visible, stage, progress }: BootLoaderProps) {
  return (
    <div className={`boot-loader${visible ? "" : " is-hidden"}`} aria-live="polite" aria-busy={visible}>
      <div className="boot-loader__inner">
        <p className="boot-loader__word">
          loading<span className="boot-loader__cursor" aria-hidden="true" />
        </p>
        <div className="boot-loader__track" aria-hidden="true">
          <div className="boot-loader__fill" style={{ width: `${Math.round(progress * 100)}%` }} />
        </div>
        <p className="boot-loader__stage">{stage}</p>
      </div>
    </div>
  );
}
