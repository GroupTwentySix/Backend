const API_URL = 'http://localhost:7000';
let token = localStorage.getItem('jwt_token');

function updateUI(isLoggedIn, isAdmin) {
    document.getElementById('auth-forms').style.display = isLoggedIn ? 'none' : 'flex';
    document.getElementById('user-info').style.display = isLoggedIn ? 'block' : 'none';
    document.getElementById('product-list').style.display = isLoggedIn ? 'block' : 'none';
    document.getElementById('basket').style.display = isLoggedIn ? 'block' : 'none';
    document.getElementById('admin-panel').style.display = isAdmin ? 'block' : 'none';
    if (isLoggedIn) {
        fetchProducts();
        const username = document.getElementById('user-info-content').dataset.username;
        if (username) {
            fetchBasket();
        }
    }
    if (isAdmin) {
        fetchAdminProducts();
    }
}


function register(event) {
    event.preventDefault();
    const username = document.getElementById('register-username').value;
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;
    const role = document.getElementById('register-role').value;

    fetch(`${API_URL}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password, role })
    })
    .then(response => response.text())
    .then(result => {
        alert(result);
        if (result.includes("Please check your email")) {
            document.getElementById('verification-message').style.display = 'block';
        }
    })
    .catch(error => console.error('Error:', error));
}

function login(event) {
    event.preventDefault();
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;

    fetch(`${API_URL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
    .then(response => response.json())
    .then(data => {
        token = data.token;
        localStorage.setItem('jwt_token', token);
        const isAdmin = data.role === 'ADMIN';
        updateUI(true, isAdmin);
        fetchUserInfo();
    })
    .catch(error => console.error('Error:', error));
}

function logout() {
    fetch(`${API_URL}/logout`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(() => {
        localStorage.removeItem('jwt_token');
        token = null;
        updateUI(false);
        document.getElementById('user-info-content').textContent = '';
        document.getElementById('admin-info').style.display = 'none';
    })
    .catch(error => console.error('Error:', error));
}

function fetchUserInfo() {
    fetch(`${API_URL}/user`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('user-info-content').textContent = `Username: ${data.username}, Role: ${data.role}`;
        document.getElementById('user-info-content').dataset.username = data.username;
        const isAdmin = data.role === 'ADMIN';
        updateUI(true, isAdmin);
        if (isAdmin) {
            fetchAdminInfo();
        }
    })
    .catch(error => console.error('Error:', error));
}


function fetchAdminInfo() {
    fetch(`${API_URL}/admin`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(response => response.text())
    .then(data => {
        document.getElementById('admin-info').style.display = 'block';
        document.getElementById('admin-info-content').textContent = data;
    })
    .catch(error => console.error('Error:', error));
}

document.getElementById('register-form').addEventListener('submit', register);
document.getElementById('login-form').addEventListener('submit', login);
document.getElementById('logout-btn').addEventListener('click', logout);

updateUI(!!token);
if (token) {
    fetchUserInfo();
}

const themeToggle = document.getElementById('theme-toggle');
const icon = themeToggle.querySelector('i');

function toggleDarkMode() {
    document.body.classList.toggle('dark-mode');
    if (document.body.classList.contains('dark-mode')) {
        icon.classList.remove('fa-moon');
        icon.classList.add('fa-sun');
        localStorage.setItem('theme', 'dark');
    } else {
        icon.classList.remove('fa-sun');
        icon.classList.add('fa-moon');
        localStorage.setItem('theme', 'light');
    }
}

themeToggle.addEventListener('click', toggleDarkMode);

// Check for saved theme preference or prefer-color-scheme
const savedTheme = localStorage.getItem('theme');
const prefersDarkScheme = window.matchMedia('(prefers-color-scheme: dark)');

if (savedTheme === 'dark' || (savedTheme === null && prefersDarkScheme.matches)) {
    document.body.classList.add('dark-mode');
    icon.classList.remove('fa-moon');
    icon.classList.add('fa-sun');
} else {
    document.body.classList.remove('dark-mode');
    icon.classList.remove('fa-sun');
    icon.classList.add('fa-moon');
}

function verifyEmail() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');
    if (token) {
        fetch(`${API_URL}/verify?token=${token}`)
        .then(response => response.text())
        .then(result => {
            alert(result);
            window.history.replaceState({}, document.title, "/");
        })
        .catch(error => console.error('Error:', error));
    }
}

verifyEmail();

function addProduct(event) {
    event.preventDefault();
    const name = document.getElementById('product-name').value;
    const description = document.getElementById('product-description').value;
    const imageUrl = document.getElementById('product-image').value;
    const price = parseFloat(document.getElementById('product-price').value);

    fetch(`${API_URL}/products`, {
        method: 'POST',
        headers: { 
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ name, description, imageUrl, price })
    })
    .then(response => response.json())
    .then(() => {
        alert('Product added successfully');
        fetchAdminProducts();
    })
    .catch(error => console.error('Error:', error));
}

function fetchProducts() {
    fetch(`${API_URL}/products`)
    .then(response => response.json())
    .then(products => {
        const container = document.getElementById('products-container');
        container.innerHTML = '';
        products.forEach(product => {
            const productElement = document.createElement('div');
            productElement.className = 'product';
            productElement.innerHTML = `
                <img src="${product.imageUrl}" alt="${product.name}">
                <h3>${product.name}</h3>
                <p>${product.description}</p>
                <p>Price: $${product.price.toFixed(2)}</p>
                <button onclick="addToBasket('${product.id}')">Add to Basket</button>
            `;
            container.appendChild(productElement);
        });
    })
    .catch(error => console.error('Error:', error));
}

function addToBasket(productId) {
    const username = document.getElementById('user-info-content').dataset.username;
    fetch(`${API_URL}/basket/${username}/${productId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(() => {
        alert('Product added to basket');
        fetchBasket();
    })
    .catch(error => console.error('Error:', error));
}

function removeFromBasket(productId) {
    const username = document.getElementById('user-info-content').dataset.username;
    fetch(`${API_URL}/basket/${username}/${productId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(() => {
        alert('Product removed from basket');
        fetchBasket();
    })
    .catch(error => console.error('Error:', error));
}

function fetchBasket() {
    const username = document.getElementById('user-info-content').dataset.username;
    if (!username) {
        console.error('Username not found');
        return;
    }
    fetch(`${API_URL}/basket/${username}`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('User not found');
        }
        return response.json();
    })
    .then(products => {
        const container = document.getElementById('basket-container');
        container.innerHTML = '';
        products.forEach(product => {
            const productElement = document.createElement('div');
            productElement.className = 'basket-item';
            productElement.innerHTML = `
                <img src="${product.imageUrl}" alt="${product.name}">
                <h3>${product.name}</h3>
                <p>Price: $${product.price.toFixed(2)}</p>
                <button onclick="removeFromBasket('${product.id}')">Remove</button>
            `;
            container.appendChild(productElement);
        });
    })
    .catch(error => {
        console.error('Error:', error.message);
        document.getElementById('basket-container').innerHTML = '<p>Unable to fetch basket</p>';
    });
}

document.getElementById('add-product-form').addEventListener('submit', addProduct);

function fetchAdminProducts() {
    fetch(`${API_URL}/products`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(response => response.json())
    .then(products => {
        const container = document.getElementById('admin-products-container');
        container.innerHTML = '';
        products.forEach(product => {
            const productElement = document.createElement('div');
            productElement.className = 'product';
            productElement.innerHTML = `
                <img src="${product.imageUrl}" alt="${product.name}">
                <h3>${product.name}</h3>
                <p>${product.description}</p>
                <p>Price: $${product.price.toFixed(2)}</p>
                <button onclick="removeProduct('${product.id}')">Remove Product</button>
            `;
            container.appendChild(productElement);
        });
    })
    .catch(error => console.error('Error:', error));
}


function removeProduct(productId) {
    fetch(`${API_URL}/products/${productId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    })
    .then(response => response.text())
    .then(result => {
        alert(result);
        fetchAdminProducts();
    })
    .catch(error => console.error('Error:', error));
}