import { FeaturePlaceholder } from "../../shared/ui/FeaturePlaceholder";

export function CheckoutPage() {
  return (
    <FeaturePlaceholder
      eyebrow="Checkout"
      title="Complete your purchase"
      description="Checkout will submit the cart to the transactional order API and clearly handle paid, declined and stock-conflict outcomes."
      nextStep="Connect the fake payment token and order confirmation flow."
    />
  );
}
