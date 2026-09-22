package br.com.doalocal.exception;

/** Indica tentativa de entregar mais itens do que o saldo disponível. */
public final class InsufficientDonationQuantityException extends RuntimeException {

    private final int availableQuantity;

    public InsufficientDonationQuantityException(int availableQuantity) {
        super("A quantidade informada é maior que a quantidade disponível para esta doação.");
        this.availableQuantity = availableQuantity;
    }

    public int getAvailableQuantity() {
        return availableQuantity;
    }
}
