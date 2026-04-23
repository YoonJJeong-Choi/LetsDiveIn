export const openCartModal = () => {
  const bootstrap = require("bootstrap"); // dynamically import bootstrap
  const modalElements = document.querySelectorAll(".modal.show");
  modalElements.forEach((modal) => {
    if (modal) {
      const modalInstance = bootstrap.Modal.getInstance(modal);
      if (modalInstance) {
        modalInstance.hide();
      }
    }
  });

  // Close any open offcanvas
  const offcanvasElements = document.querySelectorAll(".offcanvas.show");
  offcanvasElements.forEach((offcanvas) => {
    if (offcanvas) {
      const offcanvasInstance = bootstrap.Offcanvas.getInstance(offcanvas);
      if (offcanvasInstance) {
        offcanvasInstance.hide();
      }
    }
  });
  
  const shoppingCartElement = document.getElementById("shoppingCart");
  if (!shoppingCartElement) {
    console.error("shoppingCart element not found");
    return;
  }
  
  var myModal = new bootstrap.Modal(shoppingCartElement, {
    keyboard: false,
  });

  myModal.show();
  shoppingCartElement.addEventListener("hidden.bs.modal", () => {
    myModal.hide();
  });
};
