"use client";
export default function QuantitySelect({
  quantity = 1,
  setQuantity = () => {},
  styleClass = "",
  maxQuantity = null, // 최대 수량 제한 (재고 수량)
}) {
  const handleDecrease = () => {
    if (quantity > 1) {
      setQuantity(quantity - 1);
    }
  };

  const handleIncrease = () => {
    if (maxQuantity === null || quantity < maxQuantity) {
      setQuantity(quantity + 1);
    } else {
      alert(`재고가 부족합니다. (최대 ${maxQuantity}개)`);
    }
  };

  const handleChange = (e) => {
    const value = parseInt(e.target.value, 10);
    if (!isNaN(value) && value > 0) {
      if (maxQuantity !== null && value > maxQuantity) {
        alert(`재고가 부족합니다. (최대 ${maxQuantity}개)`);
        setQuantity(maxQuantity);
      } else {
        setQuantity(value);
      }
    }
  };

  return (
    <>
      <div className={`wg-quantity ${styleClass} `}>
        <span
          className="btn-quantity btn-decrease"
          onClick={handleDecrease}
          role="button"
          tabIndex={0}
        >
          -
        </span>
        <input
          className="quantity-product"
          type="number"
          name="number"
          value={quantity}
          onChange={handleChange}
          max={maxQuantity || undefined}
        />
        <span
          className="btn-quantity btn-increase"
          onClick={handleIncrease}
          role="button"
          tabIndex={0}
          style={{
            opacity: maxQuantity !== null && quantity >= maxQuantity ? 0.5 : 1,
            cursor: maxQuantity !== null && quantity >= maxQuantity ? 'not-allowed' : 'pointer'
          }}
        >
          +
        </span>
      </div>
      {maxQuantity !== null && maxQuantity !== undefined && maxQuantity === 0 && (
        <div style={{ color: '#ff4d4f', fontSize: '12px', marginTop: '4px' }}>
          품절
        </div>
      )}
    </>
  );
}
