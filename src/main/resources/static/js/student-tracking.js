(function () {
    "use strict";

    var importSemester = document.querySelector('[data-import-semester]');
    var importMonth = document.querySelector('[data-import-month]');
    if (importSemester && importMonth) {
        var updateImportMonths = function () {
            var semester = importSemester.value;
            var firstVisible = null;
            Array.prototype.forEach.call(importMonth.options, function (option) {
                var visible = option.getAttribute('data-semester') === semester;
                option.hidden = !visible;
                option.disabled = !visible;
                if (visible && firstVisible === null) firstVisible = option;
            });
            if (!importMonth.selectedOptions.length || importMonth.selectedOptions[0].disabled) {
                importMonth.value = firstVisible ? firstVisible.value : '';
            }
        };
        importSemester.addEventListener('change', updateImportMonths);
        updateImportMonths();
    }

    var searchInput = document.getElementById("classStudentSearch");
    if (!searchInput) {
        return;
    }

    var status = document.getElementById("classStudentSearchStatus");
    var rows = Array.prototype.slice.call(document.querySelectorAll(".tracking-table tbody tr"))
            .filter(function (row) {
                return row.querySelector('input[name$=".fullName"]');
            });

    function normalize(value) {
        return (value || "")
                .normalize("NFD")
                .replace(/[\u0300-\u036f]/g, "")
                .replace(/đ/g, "d")
                .replace(/Đ/g, "D")
                .toLowerCase()
                .replace(/\s+/g, " ")
                .trim();
    }

    searchInput.addEventListener("input", function () {
        var query = normalize(searchInput.value);
        var visibleCount = 0;
        rows.forEach(function (row) {
            var nameInput = row.querySelector('input[name$=".fullName"]');
            var visible = !query || normalize(nameInput.value).indexOf(query) !== -1;
            row.hidden = !visible;
            if (visible) {
                visibleCount++;
            }
        });
        status.textContent = query ? visibleCount + " học sinh phù hợp" : "";
    });
}());
