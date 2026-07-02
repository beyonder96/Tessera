// State variables
let activeListId = null;
let db = null;
let currentItems = [];
let unsubscribe = null;

// Add Modal States
let newProductName = "";
let newProductUnit = "un";
let newProductQty = 1;
let newProductPrice = 0;

// Edit Modal States
let editingItemIndex = -1;
let editingItemUnit = "un";

// Helper function to extract query parameters
function getQueryParam(name) {
    const urlParams = new URLSearchParams(window.location.search);
    return urlParams.get(name);
}

// Format double values into PT-BR currency format
function formatCurrency(val) {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
}

// Format double quantities cleanly
function formatQty(qty, unit) {
    if (unit === 'kg') {
        return new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 3, maximumFractionDigits: 3 }).format(qty);
    }
    return Math.floor(qty).toString();
}

// Initialize application
document.addEventListener("DOMContentLoaded", () => {
    activeListId = getQueryParam("listId");
    
    if (!activeListId) {
        document.body.innerHTML = `
            <div class="modal">
                <div class="glass-card" style="text-align: center;">
                    <h2>Link de Compartilhamento Inválido</h2>
                    <p style="color: #81928F; margin-bottom: 20px;">Este link não contém o identificador da lista de mercado. Por favor, abra o link gerado a partir do seu aplicativo móvel.</p>
                </div>
            </div>
        `;
        return;
    }

    setupEventListeners();
    loadFirebaseConfig();
});

// Load configuration from local storage, fallback to config.js file
function loadFirebaseConfig() {
    let config = null;

    // Check localStorage first
    const savedConfig = localStorage.getItem("tessera_firebase_config");
    if (savedConfig) {
        try {
            config = JSON.parse(savedConfig);
        } catch (e) {
            console.error("Failed to parse saved config", e);
        }
    }

    // Fallback to static config.js if it contains real data
    if (!config && typeof firebaseConfig !== 'undefined' && firebaseConfig.apiKey && firebaseConfig.apiKey !== "YOUR_API_KEY") {
        config = firebaseConfig;
    }

    if (config && config.apiKey && config.projectId) {
        initFirebase(config);
        document.getElementById("config-banner").classList.add("hidden");
    } else {
        // Show configuration banner
        document.getElementById("config-banner").classList.remove("hidden");
        document.getElementById("status-text").innerText = "Firebase não configurado";
        document.querySelector(".status-indicator").classList.add("error");
    }
}

// Initialize Firebase SDK
function initFirebase(config) {
    try {
        const indicator = document.querySelector(".status-indicator");
        indicator.className = "status-indicator connecting";
        document.getElementById("status-text").innerText = "Conectando...";

        // Check if firebase app is already initialized
        let app;
        if (!firebase.apps.length) {
            app = firebase.initializeApp(config);
        } else {
            app = firebase.app();
        }

        db = firebase.firestore(app);
        startRealtimeSync();
    } catch (e) {
        console.error("Firebase init failed", e);
        document.getElementById("status-text").innerText = "Erro na inicialização";
        document.querySelector(".status-indicator").className = "status-indicator error";
    }
}

// Listen to changes in Firestore in real-time
function startRealtimeSync() {
    if (!db || !activeListId) return;

    if (unsubscribe) unsubscribe();

    const indicator = document.querySelector(".status-indicator");

    unsubscribe = db.collection("market_lists").document(activeListId)
        .onSnapshot((doc) => {
            if (doc.exists) {
                indicator.className = "status-indicator connected";
                document.getElementById("status-text").innerText = "Conectado";
                
                const data = doc.data();
                currentItems = data.items || [];
                renderItems();
            } else {
                indicator.className = "status-indicator connected";
                document.getElementById("status-text").innerText = "Sincronizado (Vazio)";
                currentItems = [];
                renderItems();
            }
        }, (error) => {
            console.error("Firestore sync error", error);
            document.getElementById("status-text").innerText = "Erro na Sincronização";
            indicator.className = "status-indicator error";
        });
}

// Render market items on page
function renderItems() {
    const toPickList = document.getElementById("to-pick-list");
    const inCartList = document.getElementById("in-cart-list");
    
    toPickList.innerHTML = "";
    inCartList.innerHTML = "";

    const toPick = currentItems.filter(item => !item.isChecked);
    const inCart = currentItems.filter(item => item.isChecked);

    document.getElementById("to-pick-count").innerText = toPick.length;
    document.getElementById("in-cart-count").innerText = inCart.length;

    // Render A Pegar
    if (toPick.length === 0) {
        toPickList.innerHTML = `<p class="empty-text">Nenhum item a pegar.</p>`;
    } else {
        toPick.forEach((item, index) => {
            const actualIndex = currentItems.indexOf(item);
            toPickList.appendChild(createItemCard(item, actualIndex));
        });
    }

    // Render No Carrinho
    if (inCart.length === 0) {
        inCartList.innerHTML = `<p class="empty-text">Carrinho vazio.</p>`;
    } else {
        inCart.forEach((item, index) => {
            const actualIndex = currentItems.indexOf(item);
            inCartList.appendChild(createItemCard(item, actualIndex));
        });
    }

    // Update Cart Total
    const total = inCart.reduce((sum, item) => sum + (item.price * item.quantity), 0);
    document.getElementById("cart-total").innerText = formatCurrency(total);
}

// Create an Item Card Element
function createItemCard(item, index) {
    const card = document.createElement("div");
    card.className = `item-card ${item.isChecked ? 'checked' : ''}`;
    
    // Clicking the card toggles its checkbox status
    card.addEventListener("click", (e) => {
        // Do not toggle if the edit button itself was clicked
        if (e.target.closest('.btn-edit-item')) return;
        toggleItemChecked(index);
    });

    const qtyStr = formatQty(item.quantity, item.unit);
    const priceStr = formatCurrency(item.price);
    const totalStr = formatCurrency(item.price * item.quantity);
    
    const subtitle = item.price > 0 
        ? `${qtyStr} ${item.unit} × ${priceStr} = ${totalStr}` 
        : `${qtyStr} ${item.unit}`;

    card.innerHTML = `
        <div class="item-left">
            <div class="checkbox-trigger">
                <svg class="checkbox-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                </svg>
            </div>
            <div class="item-info">
                <span class="item-name">${escapeHtml(item.name)}</span>
                <span class="item-subtitle">${subtitle}</span>
            </div>
        </div>
        <button class="btn-edit-item" title="Editar item">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 20h9"></path><path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"></path></svg>
        </button>
    `;

    // Hook edit button click
    card.querySelector(".btn-edit-item").addEventListener("click", () => {
        openEditModal(index);
    });

    return card;
}

// Helper to escape HTML tags
function escapeHtml(str) {
    return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;").replace(/'/g, "&#039;");
}

// Write modifications back to Firestore
function updateFirestore() {
    if (!db || !activeListId) return;
    
    db.collection("market_lists").document(activeListId).set({
        updatedAt: Date.now(),
        items: currentItems
    }).catch(e => {
        console.error("Firestore write failed", e);
        alert("Erro ao sincronizar dados com o Firebase.");
    });
}

// Toggle checkbox state of an item
function toggleItemChecked(index) {
    currentItems[index].isChecked = !currentItems[index].isChecked;
    renderItems();
    updateFirestore();
}

// Set up UI Event listeners
function setupEventListeners() {
    // Add Item Dialog opening
    const addModal = document.getElementById("add-modal");
    document.getElementById("show-add-modal-btn").addEventListener("click", () => {
        openAddModal();
    });

    // Close Modals
    document.querySelectorAll(".cancel-modal-btn").forEach(btn => {
        btn.addEventListener("click", () => addModal.classList.add("hidden"));
    });
    
    document.querySelectorAll(".cancel-edit-btn").forEach(btn => {
        btn.addEventListener("click", () => document.getElementById("edit-modal").classList.add("hidden"));
    });

    // Step 1 input checking
    const nameInput = document.getElementById("input-prod-name");
    const nextBtn1 = document.getElementById("btn-next-step-2");
    
    nameInput.addEventListener("input", () => {
        nextBtn1.disabled = nameInput.value.trim().length === 0;
    });

    nameInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && nameInput.value.trim().length > 0) {
            goToStep(2);
        }
    });

    nextBtn1.addEventListener("click", () => {
        goToStep(2);
    });

    // Step 2 buttons
    const unBtn = document.getElementById("unit-un-btn");
    const kgBtn = document.getElementById("unit-kg-btn");

    unBtn.addEventListener("click", () => {
        newProductUnit = "un";
        unBtn.classList.add("active");
        kgBtn.classList.remove("active");
        goToStep(3);
    });

    kgBtn.addEventListener("click", () => {
        newProductUnit = "kg";
        kgBtn.classList.add("active");
        unBtn.classList.remove("active");
        goToStep(3);
    });

    document.getElementById("btn-back-step-1").addEventListener("click", () => {
        goToStep(1);
    });

    // Step 3 inputs & confirmation
    const qtyInput = document.getElementById("input-prod-qty");
    const priceInput = document.getElementById("input-prod-price");
    const confirmAddBtn = document.getElementById("btn-add-product");

    function updateStep3Total() {
        const qty = parseFloat(qtyInput.value) || 0;
        const price = parseFloat(priceInput.value) || 0;
        document.getElementById("preview-total").innerText = formatCurrency(qty * price);
        confirmAddBtn.disabled = qty <= 0 || price <= 0;
    }

    qtyInput.addEventListener("input", updateStep3Total);
    priceInput.addEventListener("input", updateStep3Total);
    priceInput.addEventListener("keydown", (e) => {
        if (e.key === "Enter" && !confirmAddBtn.disabled) {
            addNewProduct();
        }
    });

    document.getElementById("btn-back-step-2").addEventListener("click", () => {
        goToStep(2);
    });

    confirmAddBtn.addEventListener("click", () => {
        addNewProduct();
    });

    // Edit Modal actions
    document.getElementById("btn-save-edit").addEventListener("click", saveItemEdits);
    document.getElementById("btn-delete-item").addEventListener("click", deleteItem);
    
    const editQty = document.getElementById("edit-prod-qty");
    const editPrice = document.getElementById("edit-prod-price");
    const editUnBtn = document.getElementById("edit-unit-un");
    const editKgBtn = document.getElementById("edit-unit-kg");

    editUnBtn.addEventListener("click", () => {
        editingItemUnit = "un";
        editUnBtn.classList.add("active");
        editKgBtn.classList.remove("active");
        document.getElementById("edit-qty-label").innerText = "Quantidade";
    });

    editKgBtn.addEventListener("click", () => {
        editingItemUnit = "kg";
        editKgBtn.classList.add("active");
        editUnBtn.classList.remove("active");
        document.getElementById("edit-qty-label").innerText = "Peso (Kg)";
    });

    // Config Dialog actions
    document.getElementById("open-config-btn").addEventListener("click", (e) => {
        e.preventDefault();
        openConfigModal();
    });
    
    document.getElementById("close-config-btn").addEventListener("click", () => {
        document.getElementById("config-modal").classList.add("hidden");
    });

    document.getElementById("save-config-btn").addEventListener("click", () => {
        const apiKey = document.getElementById("cfg-api-key").value.trim();
        const projectId = document.getElementById("cfg-project-id").value.trim();
        const appId = document.getElementById("cfg-app-id").value.trim();

        if (!apiKey || !projectId || !appId) {
            alert("Por favor, preencha todos os campos.");
            return;
        }

        const config = {
            apiKey: apiKey,
            authDomain: `${projectId}.firebaseapp.com`,
            projectId: projectId,
            storageBucket: `${projectId}.appspot.com`,
            appId: appId
        };

        localStorage.setItem("tessera_firebase_config", JSON.stringify(config));
        document.getElementById("config-modal").classList.add("hidden");
        document.getElementById("config-banner").classList.add("hidden");
        
        initFirebase(config);
    });
}

// Open Add Product Dialog
function openAddModal() {
    newProductName = "";
    newProductUnit = "un";
    newProductQty = 1;
    newProductPrice = 0;

    document.getElementById("input-prod-name").value = "";
    document.getElementById("input-prod-qty").value = "1";
    document.getElementById("input-prod-price").value = "";
    document.getElementById("preview-total").innerText = "R$ 0,00";
    document.getElementById("btn-next-step-2").disabled = true;
    document.getElementById("btn-add-product").disabled = true;

    // Reset switches
    document.getElementById("unit-un-btn").classList.add("active");
    document.getElementById("unit-kg-btn").classList.remove("active");

    goToStep(1);
    document.getElementById("add-modal").classList.remove("hidden");
    setTimeout(() => {
        document.getElementById("input-prod-name").focus();
    }, 100);
}

// Wizard navigation
function goToStep(stepNum) {
    document.querySelectorAll(".modal-step").forEach(step => step.classList.add("hidden"));
    
    if (stepNum === 1) {
        document.getElementById("step-1").classList.remove("hidden");
    } else if (stepNum === 2) {
        newProductName = document.getElementById("input-prod-name").value.trim();
        document.getElementById("step-2").classList.remove("hidden");
    } else if (stepNum === 3) {
        document.getElementById("summary-prod-name").innerText = newProductName;
        document.getElementById("qty-label").innerText = newProductUnit === 'kg' ? "Peso (Kg)" : "Quantidade";
        document.getElementById("input-prod-qty").value = newProductUnit === 'kg' ? "0.000" : "1";
        document.getElementById("step-3").classList.remove("hidden");
        setTimeout(() => {
            document.getElementById("input-prod-price").focus();
        }, 100);
    }
}

// Add the product constructed in the modal
function addNewProduct() {
    const qty = parseFloat(document.getElementById("input-prod-qty").value) || 0;
    const price = parseFloat(document.getElementById("input-prod-price").value) || 0;

    if (!newProductName || qty <= 0 || price <= 0) return;

    const newItem = {
        name: newProductName,
        isChecked: true, // Auto check when added at the market
        isBought: false,
        price: price,
        quantity: qty,
        unit: newProductUnit,
        category: "Geral"
    };

    currentItems.push(newItem);
    renderItems();
    updateFirestore();

    document.getElementById("add-modal").classList.add("hidden");
}

// Open Edit Dialog for a specific item
function openEditModal(index) {
    editingItemIndex = index;
    const item = currentItems[index];

    document.getElementById("edit-prod-name").innerText = item.name;
    document.getElementById("edit-prod-qty").value = item.quantity;
    document.getElementById("edit-prod-price").value = item.price;
    
    editingItemUnit = item.unit;
    const unBtn = document.getElementById("edit-unit-un");
    const kgBtn = document.getElementById("edit-unit-kg");

    if (editingItemUnit === "un") {
        unBtn.classList.add("active");
        kgBtn.classList.remove("active");
        document.getElementById("edit-qty-label").innerText = "Quantidade";
    } else {
        kgBtn.classList.add("active");
        unBtn.classList.remove("active");
        document.getElementById("edit-qty-label").innerText = "Peso (Kg)";
    }

    document.getElementById("edit-modal").classList.remove("hidden");
}

// Save edits made to item
function saveItemEdits() {
    if (editingItemIndex < 0) return;

    const qty = parseFloat(document.getElementById("edit-prod-qty").value) || 0;
    const price = parseFloat(document.getElementById("edit-prod-price").value) || 0;

    if (qty <= 0) {
        alert("A quantidade deve ser maior do que zero.");
        return;
    }

    const item = currentItems[editingItemIndex];
    item.quantity = qty;
    item.price = price;
    item.unit = editingItemUnit;

    renderItems();
    updateFirestore();

    document.getElementById("edit-modal").classList.add("hidden");
}

// Delete item entirely from list
function deleteItem() {
    if (editingItemIndex < 0) return;
    
    if (confirm(`Excluir "${currentItems[editingItemIndex].name}" da lista?`)) {
        currentItems.splice(editingItemIndex, 1);
        renderItems();
        updateFirestore();
        document.getElementById("edit-modal").classList.add("hidden");
    }
}

// Open Configuration Dialog
function openConfigModal() {
    document.getElementById("config-modal").classList.remove("hidden");
    
    const savedConfig = localStorage.getItem("tessera_firebase_config");
    if (savedConfig) {
        const config = JSON.parse(savedConfig);
        document.getElementById("cfg-api-key").value = config.apiKey || "";
        document.getElementById("cfg-project-id").value = config.projectId || "";
        document.getElementById("cfg-app-id").value = config.appId || "";
    }
}
