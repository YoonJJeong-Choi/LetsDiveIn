import PolicyPage from "@/components/otherPages/PolicyPage";
import { policyPages } from "@/data/policyPages";

export const metadata = {
  title: "반품 및 환불 || Let’s Dive In",
  description: "스윔몰 반품 및 환불 안내",
};

export default function ReturnsRefundsPage() {
  return <PolicyPage {...policyPages.returnsRefunds} />;
}
