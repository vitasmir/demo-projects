import { Suspense } from "react";
import { StandingOrderFormPage } from "../../payment-pages";

export default function Page() {
  return <Suspense fallback={null}><StandingOrderFormPage /></Suspense>;
}
