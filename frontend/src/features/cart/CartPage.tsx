import { FeaturePlaceholder } from "../../shared/ui/FeaturePlaceholder";

export function CartPage() {
  return (
    <FeaturePlaceholder
      eyebrow="Cart"
      title="Review your selection"
      description="Cart state will remain client-side while the backend stays authoritative for availability and final pricing."
      nextStep="Add quantity controls, totals and the transition into checkout."
    />
  );
}
