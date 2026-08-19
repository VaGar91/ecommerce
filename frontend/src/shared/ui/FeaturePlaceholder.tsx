type FeaturePlaceholderProps = {
  eyebrow: string;
  title: string;
  description: string;
  nextStep: string;
};

export function FeaturePlaceholder({
  eyebrow,
  title,
  description,
  nextStep,
}: FeaturePlaceholderProps) {
  return (
    <section className="feature-page">
      <div className="feature-intro">
        <p className="eyebrow">{eyebrow}</p>
        <h1>{title}</h1>
        <p className="lede">{description}</p>
      </div>

      <aside className="foundation-note">
        <span>Foundation ready</span>
        <p>{nextStep}</p>
      </aside>
    </section>
  );
}
