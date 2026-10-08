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

    var attendancePlaceholders = {
        ABSENT: "V.08/10, V.15/10",
        DROPPED_OUT: "BH.08/10, BH.15/10",
        TRANSFERRED: "CT.08/10, CT.15/10"
    };
    var attendanceSelects = document.querySelectorAll('select[name$=".attendanceStatus"]');

    function updateAttendanceDates(select) {
        var row = select.closest("tr");
        var datesInput = row && row.querySelector('input[name$=".attendanceDates"]');
        if (!datesInput) {
            return;
        }
        var placeholder = attendancePlaceholders[select.value];
        datesInput.disabled = !placeholder;
        datesInput.required = Boolean(placeholder);
        datesInput.placeholder = placeholder || "Không cần nhập ngày";
        if (!placeholder) {
            datesInput.value = "";
        }
    }

    Array.prototype.forEach.call(attendanceSelects, function (select) {
        updateAttendanceDates(select);
        select.addEventListener("change", function () {
            updateAttendanceDates(select);
        });
    });

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
