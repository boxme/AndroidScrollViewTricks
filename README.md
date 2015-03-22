# AndroidScrollViewTricks

There are two fragments included in this project: QuickReturnFragment and StickyHeaderFragment.
Both use an ObservableScrollView that can accept a scroll listener

### QuickReturnFragment
* A view that will drop down or go up quickly based on the direction of scroll. A common use case is for the disappearing/appearing actionbar in any scrollable view.

### StickyHeaderFragment
* A view that will scroll along with the rest of the scrollable children, until it reaches the top of the screen. It will then stick at the top while the rest of the children scroll.
