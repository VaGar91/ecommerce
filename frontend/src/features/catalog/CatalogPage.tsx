import { FeaturePlaceholder } from "../../shared/ui/FeaturePlaceholder";

export function CatalogPage() {
  return (
    <FeaturePlaceholder
      eyebrow="Storefront"
      title="Find the right product"
      description="The catalog route is wired and ready for URL-driven search, category filters, pagination and cart actions."
      nextStep="Connect this page to GET /api/products and render the first product grid."
    />
  );
}
