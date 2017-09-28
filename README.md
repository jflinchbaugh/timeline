# timeline

generated using Luminus version "2.9.11.89"

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

## Running

To start a web server for the application, run:

    lein run 

## Local configs

I've (unconventionally) moved the dev and test DB environments
into the project.clj file, 
since those can be shared across developers
and contain no proprietary information.

At some point, you may want a `profiles.clj` 
in the root of the project
for local configs that won't get versioned.
Start with at least an empty map (`{}`), 
if you create the file.
