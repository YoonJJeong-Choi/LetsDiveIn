import PolicyPage from "@/components/otherPages/PolicyPage";
import { policyPages } from "@/data/policyPages";

export const metadata = {
  title: "개인정보 처리방침 || Let’s Dive In",
  description: "스윔몰 개인정보 처리방침",
};

export default function PrivacyPolicyPage() {
  return <PolicyPage {...policyPages.privacyPolicy} />;
}
