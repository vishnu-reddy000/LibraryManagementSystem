const form = document.querySelector("form");

form.addEventListener("submit", (e) => {
    const pass = document.querySelector('input[name="password"]').value;
    const confirmInput = document.querySelector('input[name="confirmPassword"]');
    const confirm = confirmInput.value;

    if (pass !== confirm) {
        e.preventDefault();
        confirmInput.setCustomValidity("Passwords do not match.");
        confirmInput.reportValidity();
    } else {
        confirmInput.setCustomValidity("");
    }
});

document.querySelector('input[name="confirmPassword"]').addEventListener('input', function() {
    this.setCustomValidity("");
});