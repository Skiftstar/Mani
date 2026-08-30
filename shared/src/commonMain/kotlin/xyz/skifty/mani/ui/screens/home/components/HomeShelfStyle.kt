package xyz.skifty.mani.ui.screens.home.components

/** Whether [HomeShelf] shows its prev/next paging arrows - true on desktop (no touch input, so
 *  a mouse-driven affordance is the only way to move the row besides a trackpad/scroll-wheel),
 *  false on Android (dragging the row directly with a finger already scrolls it - free scrolling,
 *  not paging - so the arrows would just be redundant chrome). */
expect val showHomeShelfPageArrows: Boolean
