import PolicyPage from "@/components/otherPages/PolicyPage";
import { policyPages } from "@/data/policyPages";

export const metadata = {
  title: "배송 안내 || Let’s Dive In",
  description: "스윔몰 배송 안내",
};

export default function DeliveryInfoPage() {
  return <PolicyPage {...policyPages.deliveryInfo} />;
}
