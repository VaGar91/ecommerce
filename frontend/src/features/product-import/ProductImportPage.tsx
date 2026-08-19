import { FeaturePlaceholder } from "../../shared/ui/FeaturePlaceholder";

export function ProductImportPage() {
  return (
    <FeaturePlaceholder
      eyebrow="Bulk operations"
      title="Import product data"
      description="The optional import screen will expose the backend CSV report without hiding row-level validation errors."
      nextStep="Add file selection, upload progress and an accessible error summary."
    />
  );
}
