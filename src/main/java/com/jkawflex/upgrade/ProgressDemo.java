package com.jkawflex.upgrade;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public class ProgressDemo {
    public static String printProgress(long startTime, long total, long current) {
        long eta = current == 0 ? 0 :
                (total - current) * (System.currentTimeMillis() - startTime) / current;

        String etaHms = current == 0 ? "N/A" :
                String.format("%02d:%02d:%02d", TimeUnit.MILLISECONDS.toHours(eta),
                        TimeUnit.MILLISECONDS.toMinutes(eta) % TimeUnit.HOURS.toMinutes(1),
                        TimeUnit.MILLISECONDS.toSeconds(eta) % TimeUnit.MINUTES.toSeconds(1));

        StringBuilder string = new StringBuilder(35);
        int percent = (int) (current * 100 / total)/4;
        string
                .append('\r')
                .append(String.join("", Collections.nCopies(percent == 0 ? 2 : 2 - (int) (Math.log10(percent)), " ")))
                .append(String.format(" %d%% [", percent*4))
                .append(String.join("", Collections.nCopies(percent, "-")))
                .append('-')
                .append(String.join("", Collections.nCopies(25 - (percent), " ")))
                .append(']')
                .append(String.join("", Collections.nCopies((int) (Math.log10(total)) - (int) (Math.log10(current)), " ")))
                .append(String.format(" %d/%d, ETA: %s", current, total, etaHms));

       return string.toString()+"   ";
    }

  public static void main(String[] args) {
    try {
        long total = 5000;
        long startTime = System.currentTimeMillis();

        for (int i = 1; i <= total; i = i + 3) {
            try {
                Thread.sleep(2);
                System.out.print(printProgress(startTime, total, i));
            } catch (InterruptedException e) {
            }
        }
    } catch (Exception e) {}
  }
}