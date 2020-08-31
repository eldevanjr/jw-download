package com.jkawflex.upgrade;

import lombok.Data;

import java.util.concurrent.Future;

@Data
abstract class FutureRunnable implements Runnable {

    private Future<?> future;

    /* Getter and Setter for future */

}