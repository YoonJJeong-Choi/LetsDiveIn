import MyAccountAuthGate from "@/components/my-account/MyAccountAuthGate";

export default function MyAccountLayout({ children }) {
  return <MyAccountAuthGate>{children}</MyAccountAuthGate>;
}
