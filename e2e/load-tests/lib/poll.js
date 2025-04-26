import { sleep } from "k6";

// Modified from https://davidwalsh.name/javascript-polling.
function poll(fn, callback, errback, maxRetries) {
  let retryCounter = 0;

  (function func() {
    retryCounter++;
    // If the condition is met, we're done!
    if (fn()) {
      callback();
    }
    // If the condition isn't met and we reached 'maxRetries', go again.
    else if (retryCounter < maxRetries) {
      sleep(1);
      func();
    }
    // Didn't match and we reached 'maxRetries', reject!
    else {
      errback();
    }
  })();
}

export { poll };
