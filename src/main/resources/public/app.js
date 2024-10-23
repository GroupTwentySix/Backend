const API_URL = 'http://localhost:7000';
let token = localStorage.getItem('jwt_token');

// Updates the UI based on login and admin status
function updateUI(isLoggedIn, isAdmin) {
    // Show or hide elements based on user login status
    document.getElementById('auth-forms').style.display = isLoggedIn ? 'none' : 'flex';
    document.getElementById('user-info').style.display = isLoggedIn ? 'block' : 'none';
    document.getElementById('product-list').style.display = isLoggedIn ? 'block' : 'none';
    document.getElementById('basket').style.display = isLoggedIn ? 'block' : 'none';
    document.getElementById('admin-panel').style.display = isAdmin ? 'block' : 'none';

    if (isLoggedIn) {
        fetchProducts(); // Load products if user is logged in

        const username = document.getElementById('user-info-content').dataset.username;
        if (username) {
            fetchBasket(); // Load user's basket if a username is available
        }
    }

    if (isAdmin) {
        fetchAdminProducts(); // Load admin-specific product management if user is an admin
    }
}

// Handles user registration
function register(event) {
    event.preventDefault(); // Prevent default form submission

    // Capture user inputs from the form
    const username = document.getElementById('register-username').value;
    const email = document.getElementById('register-email').value;
    const password = document.getElementById('register-password').value;
    const role = document.getElementById('register-role').value;

    // Send registration data to the backend
    fetch(`${API_URL}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, email, password, role })
    })
        .then(response => response.text())
        .then(result => {
            alert(result); // Show registration result

            // If registration requires email verification, show verification message
            if (result.includes("Please check your email")) {
                document.getElementById('verification-message').style.display = 'block';
            }
        })
        .catch(error => console.error('Error:', error));
}

// Handles user login
function login(event) {
    event.preventDefault(); // Prevent default form submission

    // Capture username and password inputs from the form
    const username = document.getElementById('login-username').value;
    const password = document.getElementById('login-password').value;

    // Send login data to the backend
    fetch(`${API_URL}/login`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username, password })
    })
        .then(response => response.json())
        .then(data => {
            // Store the JWT token locally
            token = data.token;
            localStorage.setItem('jwt_token', token);

            // Check if the user is an admin
            const isAdmin = data.role === 'ADMIN';

            // Update the UI and fetch user information
            updateUI(true, isAdmin);
            fetchUserInfo();
        })
        .catch(error => console.error('Error:', error));
}

// Handles user logout
function logout() {
    // Call backend to perform logout
    fetch(`${API_URL}/logout`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(() => {
            // Clear local storage and token, update the UI to reflect logged-out status
            localStorage.removeItem('jwt_token');
            token = null;
            updateUI(false);

            // Clear user info display and hide admin info
            document.getElementById('user-info-content').textContent = '';
            document.getElementById('admin-info').style.display = 'none';
        })
        .catch(error => console.error('Error:', error));
}

// Fetches user information from the server
function fetchUserInfo() {
    fetch(`${API_URL}/user`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(response => response.json())
        .then(data => {
            // Display the user's information and update the dataset
            document.getElementById('user-info-content').textContent = `Username: ${data.username}, Role: ${data.role}`;
            document.getElementById('user-info-content').dataset.username = data.username;

            const isAdmin = data.role === 'ADMIN';
            updateUI(true, isAdmin);

            if (isAdmin) {
                fetchAdminInfo(); // Fetch admin info if user is admin
            }
        })
        .catch(error => console.error('Error:', error));
}

// Fetches admin-specific information
function fetchAdminInfo() {
    fetch(`${API_URL}/admin`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(response => response.text())
        .then(data => {
            // Display admin information
            document.getElementById('admin-info').style.display = 'block';
            document.getElementById('admin-info-content').textContent = data;
        })
        .catch(error => console.error('Error:', error));
}

// Add event listeners for registration, login, and logout
document.getElementById('register-form').addEventListener('submit', register);
document.getElementById('login-form').addEventListener('submit', login);
document.getElementById('logout-btn').addEventListener('click', logout);

// Update UI on page load based on whether a token exists
updateUI(!!token);
if (token) {
    fetchUserInfo();
}

// Dark mode toggle functionality
const themeToggle = document.getElementById('theme-toggle');
const icon = themeToggle.querySelector('i');

// Toggle between light and dark modes
function toggleDarkMode() {
    document.body.classList.toggle('dark-mode');

    // Change the icon based on the current theme
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

// Event listener for the dark mode toggle
themeToggle.addEventListener('click', toggleDarkMode);

// Load saved theme preference or use the system's preferred color scheme
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

// Handles email verification when users click the link in their email
function verifyEmail() {
    const urlParams = new URLSearchParams(window.location.search);
    const token = urlParams.get('token');

    // If there's a verification token in the URL, verify it
    if (token) {
        fetch(`${API_URL}/verify?token=${token}`)
            .then(response => response.text())
            .then(result => {
                alert(result); // Show verification result
                window.history.replaceState({}, document.title, "/"); // Clear the token from the URL
            })
            .catch(error => console.error('Error:', error));
    }
}

verifyEmail(); // Verify email when page loads

// Handles adding a new product
function addProduct(event) {
    event.preventDefault(); // Prevent form submission

    // Collect product details from the form
    const name = document.getElementById('product-name').value;
    const description = document.getElementById('product-description').value;
    const imageUrl = document.getElementById('product-image').value;
    const price = parseFloat(document.getElementById('product-price').value);

    // Send product data to the backend to add it
    fetch(`${API_URL}/products`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}` // Use the stored JWT token
        },
        body: JSON.stringify({ name, description, imageUrl, price })
    })
        .then(response => response.json())
        .then(() => {
            alert('Product added successfully');
            fetchAdminProducts(); // Refresh the list of admin products after adding
        })
        .catch(error => console.error('Error:', error));
}

// Fetches products and displays them on the UI
function fetchProducts() {
    fetch(`${API_URL}/products`)
        .then(response => response.json())
        .then(products => {
            const container = document.getElementById('products-container');
            container.innerHTML = ''; // Clear current products

            // Display each product dynamically
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

// Adds a product to the user's basket
function addToBasket(productId) {
    const username = document.getElementById('user-info-content').dataset.username;

    // Send request to add the product to the user's basket
    fetch(`${API_URL}/basket/${username}/${productId}`, {
        method: 'POST',
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(() => {
            alert('Product added to basket');
            fetchBasket(); // Refresh basket after adding the product
        })
        .catch(error => console.error('Error:', error));
}

// Removes a product from the user's basket
function removeFromBasket(productId) {
    const username = document.getElementById('user-info-content').dataset.username;

    // Send request to remove the product from the basket
    fetch(`${API_URL}/basket/${username}/${productId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(() => {
            alert('Product removed from basket');
            fetchBasket(); // Refresh basket after removing the product
        })
        .catch(error => console.error('Error:', error));
}

// Fetches the user's basket and displays the products in it
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
            container.innerHTML = ''; // Clear current basket

            // Display each product in the basket dynamically
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

// Adds event listener for adding products (admin feature)
document.getElementById('add-product-form').addEventListener('submit', addProduct);

// Fetches admin-specific products for management
function fetchAdminProducts() {
    fetch(`${API_URL}/products`, {
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(response => response.json())
        .then(products => {
            const container = document.getElementById('admin-products-container');
            container.innerHTML = ''; // Clear current products

            // Display each product dynamically for admin management
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

// Removes a product (admin feature)
function removeProduct(productId) {
    // Send request to delete the product
    fetch(`${API_URL}/products/${productId}`, {
        method: 'DELETE',
        headers: { 'Authorization': `Bearer ${token}` }
    })
        .then(response => response.text())
        .then(result => {
            alert(result); // Show deletion result
            fetchAdminProducts(); // Refresh admin product list after removal
        })
        .catch(error => console.error('Error:', error));
}
