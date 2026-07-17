/* ═══════════════════════════════════════
   USER DASHBOARD — Interactive Scripts
   ═══════════════════════════════════════ */

// ── Book Carousel Smooth Scrolling ──
function scrollCarousel(dir) {
    const carousel = document.getElementById('dbCarousel');
    if (!carousel) return;
    const cardWidth = carousel.querySelector('.db-book-card')?.offsetWidth || 175;
    const gap = 32; // match layout gap
    carousel.scrollBy({ left: dir * (cardWidth + gap) * 2, behavior: 'smooth' });
}

// ── Fetch New Reading Inspiration Quote (AJAX Fetch API) ──
function fetchNewQuote(event) {
    if (event) {
        event.stopPropagation();
        event.preventDefault();
    }
    
    const quoteCard = document.getElementById('dbQuoteCard');
    const quoteText = document.getElementById('dbQuoteText');
    const quoteAuthor = document.getElementById('dbQuoteAuthor');
    const refreshBtn = event ? event.currentTarget : null;
    
    if (!quoteText || !quoteAuthor) return;

    // Spin the refresh button icon
    if (refreshBtn) {
        const icon = refreshBtn.querySelector('i');
        if (icon) {
            icon.style.transition = 'transform 0.6s cubic-bezier(0.4, 0, 0.2, 1)';
            icon.style.transform = 'rotate(360deg)';
            setTimeout(() => {
                icon.style.transition = 'none';
                icon.style.transform = '';
            }, 600);
        }
        refreshBtn.disabled = true;
    }

    // Smoothly fade out content
    quoteText.style.opacity = '0';
    quoteAuthor.style.opacity = '0';

    // Delay content replacement slightly to match fade-out
    setTimeout(() => {
        fetch('/api/quotes/random')
            .then(response => {
                if (!response.ok) throw new Error("Failed to fetch quote");
                return response.json();
            })
            .then(data => {
                if (data && data.text) {
                    quoteText.textContent = data.text;
                    quoteAuthor.textContent = data.author ? `— ${data.author}` : "— Unknown";
                }
            })
            .catch(err => {
                console.error("Error loading new quote: ", err);
                // Graceful fallback
                quoteText.textContent = "Books are uniquely portable magic.";
                quoteAuthor.textContent = "— Stephen King";
            })
            .finally(() => {
                // Smoothly fade back in
                quoteText.style.opacity = '1';
                quoteAuthor.style.opacity = '1';
                if (refreshBtn) {
                    refreshBtn.disabled = false;
                }
            });
    }, 250);
}

// Auto-run load setups
document.addEventListener('DOMContentLoaded', () => {
    // Force quote texts and authors to support transitions
    const quoteText = document.getElementById('dbQuoteText');
    const quoteAuthor = document.getElementById('dbQuoteAuthor');
    if (quoteText) quoteText.style.transition = 'opacity 0.25s ease';
    if (quoteAuthor) quoteAuthor.style.transition = 'opacity 0.25s ease';
});
