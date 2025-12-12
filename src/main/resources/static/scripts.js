// scripts.js — FINAL WITH DEBUG LOGS

const API = '';

// LOGIN
if (document.getElementById('loginForm')) {
    document.getElementById('loginForm').onsubmit = function(e) {
        e.preventDefault();
        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        if (username === 'admin' && password === 'admin123') {
            localStorage.setItem('loggedIn', 'true');
            window.location.href = 'upload.html';
        } else {
            alert('Wrong username or password!');
        }
    };
}

// UPLOAD
async function uploadFile() {
    const fileInput = document.getElementById('excelFile');
    if (!fileInput.files[0]) return alert('Select Excel file');
    const formData = new FormData();
    formData.append('file', fileInput.files[0]);

    try {
        const res = await fetch('/upload', { method: 'POST', body: formData });
        alert(await res.text());
    } catch (err) {
        alert('Upload failed');
    }
}

// DASHBOARD LOAD — WITH DEBUG LOGS
async function loadDashboard(from = '', to = '') {
    let url = '/api/dashboard';
    if (from && to) url += `?from=${from}&to=${to}`;

    console.log('Fetching data from: ' + url);  // DEBUG

    try {
        const res = await fetch(url);
        if (!res.ok) {
            console.error('API error: ' + res.status);  // DEBUG
            alert('Failed to load data');
            return;
        }
        const data = await res.json();
        console.log('Data fetched: ', data);  // DEBUG — CHECK IF EMPTY

        const tbody = document.getElementById('tableBody');
        tbody.innerHTML = '';
        data.forEach(emp => {
            const tr = document.createElement('tr');
            if (emp.shortfallDays > 0) tr.classList.add('shortfall');
            tr.innerHTML = `
                <td>${emp.employeeId}</td>
                <td>${emp.employeeName}</td>
                <td>${emp.attendedDays}</td>
                <td>${emp.avgHours.toFixed(2)}</td>
                <td><b>${emp.shortfallDays}</b></td>  <!-- UPDATED TO DAYS -->
                <td>${emp.remainderStatus}</td>
                <td><button class="btn btn-sm btn-warning" onclick="sendReminder('${emp.employeeId}')">Send Reminder</button></td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('Load error: ', err);  // DEBUG
        alert('Connection error');
    }
}

// SEND REMINDER
async function sendReminder(empId) {
    if (!localStorage.getItem('loggedIn')) return alert('Login first');
    try {
        await fetch(`/send-reminder/${empId}`, { method: 'POST' });
        alert('Reminder sent!');
        loadDashboard();
    } catch (err) {
        alert('Send failed');
    }
}

// SEARCH
function filterTable() {
    const term = document.getElementById('search')?.value.toLowerCase() || '';
    document.querySelectorAll('#dashTable tbody tr').forEach(row => {
        row.style.display = row.textContent.toLowerCase().includes(term) ? '' : 'none';
    });
}

// DATE FILTER
function applyFilter() {
    const from = document.getElementById('fromDate').value;
    const to = document.getElementById('toDate').value;
    loadDashboard(from, to);
}

// DOWNLOAD
function downloadExcel() {
    const from = document.getElementById('fromDate').value;
    const to = document.getElementById('toDate').value;
    let url = '/api/export-excel';
    if (from || to) url += `?from=${from}&to=${to}`;
    window.location.href = url;
}

// AUTO LOAD
if (window.location.pathname.includes('dashboard.html')) loadDashboard();