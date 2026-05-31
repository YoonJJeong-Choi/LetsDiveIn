import PolicyPage from "@/components/otherPages/PolicyPage";
import { policyPages } from "@/data/policyPages";

export const metadata = {
  title: "이용약관 || Let’s Dive In",
  description: "이용약관",
};

export default function TermsOfUsePage() {
  return <PolicyPage {...policyPages.termsOfUse} />;
}
