var loader = document.getElementById("preloader");
window.addEventListener("load", vanish)
function vanish() {
    loader.classList.add("display");
}
setTimeout(function () {
    $("#preloader").fadeToggle();
}, 1500);