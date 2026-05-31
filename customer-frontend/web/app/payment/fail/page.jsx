export const metadata = {
  title: "결제 실패 || Let’s Dive In",
  description: "결제가 완료되지 않았습니다.",
};

export default function PaymentFail({ searchParams }) {
  const code = searchParams?.code;
  const message = searchParams?.message;
  const orderId = searchParams?.orderId;
  console.log('[Payment Fail] params =>', { code, message, orderId });
  return (
    <div className="container py-5">
      <h1>결제 실패</h1>
      <p>결제가 완료되지 않았습니다.</p>
      <pre>{JSON.stringify({ code, message, orderId }, null, 2)}</pre>
    </div>
  );
}

