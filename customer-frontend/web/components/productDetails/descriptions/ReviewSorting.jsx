import React, { useState } from "react";

export default function ReviewSorting({ value = "latest", onChange }) {
  const [selectedOption, setSelectedOption] = useState(
    value === "latest" ? "최신순" : value === "rating_high" ? "평점 높은순" : "평점 낮은순"
  );

  const handleSelect = (option, sortValue) => {
    setSelectedOption(option);
    if (onChange) {
      onChange(sortValue);
    }
  };

  const options = [
    { label: "최신순", value: "latest" },
    { label: "평점 높은순", value: "rating_high" },
    { label: "평점 낮은순", value: "rating_low" },
  ];

  return (
    <div className="tf-dropdown-sort" data-bs-toggle="dropdown">
      <div className="btn-select">
        <span className="text-sort-value">{selectedOption}</span>
        <span className="icon icon-arrow-down" />
      </div>
      <div className="dropdown-menu">
        {options.map((option) => (
          <div
            key={option.value}
            className={`select-item ${
              value === option.value ? "active" : ""
            }`}
            onClick={() => handleSelect(option.label, option.value)}
          >
            <span className="text-value-item">{option.label}</span>
          </div>
        ))}
      </div>
    </div>
  );
}
