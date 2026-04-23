export const openWistlistModal = () => {
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
  
  const wishlistElement = document.getElementById("wishlist");
  if (!wishlistElement) {
    console.error("wishlist element not found");
    return;
  }
  
  var myModal = new bootstrap.Modal(wishlistElement, {
    keyboard: false,
  });

  myModal.show();
  wishlistElement.addEventListener("hidden.bs.modal", () => {
    myModal.hide();
  });
};
