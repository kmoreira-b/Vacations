// date guards
(function () {
    const start = document.getElementById('startDate');
    const end = document.getElementById('endDate');

    function todayStr() { return new Date().toISOString().slice(0, 10); }
    function syncMin() {
        const min = start?.value || todayStr();
        if (end) {
            end.min = min;
            if (end.value && end.value < min) end.value = min;
        }
    }
    if (start && end) {
        start.addEventListener('change', syncMin);
        syncMin();
    }

    // auto-close flash alerts
    setTimeout(() => {
        document.querySelectorAll('.alert').forEach(el => {
            try { bootstrap.Alert.getOrCreateInstance(el).close(); } catch(_) {}
        });
    }, 4000);

    // show overlay & button spinner on submit
    const form = document.getElementById('vacationForm');
    const btn = document.getElementById('bookBtn');
    const btnSpinner = document.getElementById('bookBtnSpinner');
    const overlay = document.getElementById('loadingOverlay');

    if (form && btn && btnSpinner && overlay) {
        form.addEventListener('submit', (e) => {
            // Let browser HTML5 validation run; if invalid, don't show spinner
            if (!form.checkValidity()) return;
            btn.disabled = true;
            btnSpinner.classList.remove('d-none');
            overlay.classList.remove('d-none');
        });
    }
})();
