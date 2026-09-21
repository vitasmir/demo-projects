import { Suspense } from "react";
import { TemplateFormPage } from "../../payment-pages";

export default function Page() {
  return <Suspense fallback={null}><TemplateFormPage /></Suspense>;
}
