package br.com.doalocal.exception;

/** Impede reduzir a quantidade original para menos do que o total já entregue. */
public final class DonationQuantityBelowDeliveredException extends RuntimeException {

    private final int deliveredQuantity;

    public DonationQuantityBelowDeliveredException(int deliveredQuantity) {
        super("A quantidade original não pode ser menor que a quantidade já entregue.");
        this.deliveredQuantity = deliveredQuantity;
    }

    public int getDeliveredQuantity() {
        return deliveredQuantity;
    }
}
