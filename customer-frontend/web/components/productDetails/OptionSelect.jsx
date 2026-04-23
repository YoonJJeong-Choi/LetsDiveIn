"use client";

import React from "react";

export default function OptionSelect({ 
  options = [], 
  selectedOptionNo, 
  onSelectOption,
  basePrice 
}) {
  if (!options || options.length === 0) {
    return null;
  }

  const selectedOption = options.find(opt => opt.optionNo === selectedOptionNo);

  return (
    <div className="variant-picker-item">
      <div className="d-flex justify-content-between mb_12">
        <div className="variant-picker-label">
          옵션 선택:
          {selectedOption && (
            <span className="text-title variant-picker-label-value">
              {selectedOption.optionName}
            </span>
          )}
        </div>
      </div>
      <div className="variant-picker-values variant-other-size">
        {options.map((option) => {
          const isSelected = selectedOptionNo === option.optionNo;
          
          return (
            <div
              key={option.optionNo}
              className={`btn-size other-variant-btn ${
                isSelected ? "active" : ""
              }`}
              onClick={() => onSelectOption(option.optionNo)}
              style={{
                display: "flex",
                flexDirection: "column",
                alignItems: "center",
                justifyContent: "center",
                minWidth: "80px",
                padding: "12px 16px",
              }}
            >
              <span style={{ 
                fontSize: "16px", 
                lineHeight: "24px", 
                fontWeight: "500",
                whiteSpace: "nowrap"
              }}>
                {option.optionName}
              </span>
              {option.optionAddPrice > 0 && (
                <span 
                  style={{ 
                    fontSize: "12px",
                    color: "#888",
                    fontWeight: "400",
                    marginTop: "4px",
                    lineHeight: "16px"
                  }}
                >
                  +₩{option.optionAddPrice.toLocaleString()}
                </span>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}
