# Timeline Game

## The Deck of Cards
- The game cards each have a date (year, year-month, year-month-day)
- Those values on the card are based on an input file.
- The input file is a collection of events or dates for a certain theme.
- The input can be changed to change the theme of the game.
- There will generally be hundreds of cards.

## Setup
- ask the users for names. as many names as needed, "start" the game with a button.
- The deck of cards is shuffled.
- Each Player is dealt a hand of 10 cards, face-down, they can't see them.
- The game shares a list of cards in order: the timeline.
  - It is initialized with one card from the deck, face-up,
    showing title, date, and description.
  - Time will extend from earliest to latest in the list from top to bottom.

## Play
- The current player reveals the title of the event on their top card.
- Seeing only the title, the player chooses where in the shared timeline
  they think their new event falls.
- Upon choosing, the date and description are revealed.
  - if it's in the right spot, it stays in the timeline.
  - if it's in the wrong spot, the card is discarded from the game,
    and the player draws a new card from the deck to go into their hand.

## End of Game
- When a player runs out of cards, they win.
- allow a restart: prompt for names again.

# Style
- use larger fonts, and larger components to facilitate touch
- round the corners of the cards
- show some nice, but quick, transition animations

# Technical Specs
- implement the game as a single-page application in the browser
- use clojurescript, shadow-cljs, helix
- the namespaces should start with "com.hjsoft.timeline"
- the input file will be json loaded from a remote url.
  - you can specify the json format expected.
  - create your own sample data
    - at least 3 sets
    - each sample set should contain at least 50 events
      - real-world events are best
      - if you end up generating numbered event data,
        then include the dates in the title,
        so we can use them for manual testing
    - give me a way to choose the json file, probably in the URL or something like that,
      so i can link directly to it
  - serve the data from the test serve alongside index.html
  - the json file includes color scheme/theme parameters
- all the players will share the same screen to play and take turns
- rotate through players one by one
  - show their name, and the title of their next card
  - let them click the spot between, before, or after the other timeline cards
    to specify the location where they think it fits in the timeline
  - show if they were right or wrong
  - wait for a click to proceed to next player
- when there's a winner (first player out of cards), show the winner
- allow start of a new game
- write tests for the code
  - use react testing library
  - include tests for the react components to ensure their interactions work
  - maximize test coverage
- compile and test the code to ensure it's working
  - keep running the compilation and tests until it all works
  - really really run the compiles and tests and make fixes,
    so this thing works when I run it.
- npx shadow-cljs watch should serve the page to open the app.
- see if there are any cleanups, refactoring that should be done to make it
  nicer: be proud of your work
- last thing you should do is run the compile and run the tests
  before claiming it's done.
- start by using latest versions of all libraries
  - run `clj -X:search/outdated` to verify and find new versions of clojure dependencies,
    and then apply those updates to the manifest files manually.
  - make sure you're using latest dependencies before writing code
- debug and fix any warnings during compilation. JVM warnings are OK.
