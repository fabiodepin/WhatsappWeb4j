package it.auties.whatsapp.api;

import it.auties.whatsapp.controller.Keys;
import it.auties.whatsapp.controller.Store;
import it.auties.whatsapp.model.mobile.PhoneNumber;
import it.auties.whatsapp.model.mobile.VerificationCodeMethod;
import it.auties.whatsapp.registration.HttpRegistration;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

/**
 * A builder to specify the options for the mobile api
 */
@SuppressWarnings("unused")
public sealed class MobileRegistrationBuilder<T extends MobileRegistrationBuilder<T>> {
    final Store store;
    final Keys keys;
    final ErrorHandler errorHandler;
    final ExecutorService socketExecutor;
    Whatsapp whatsapp;
    AsyncVerificationCodeSupplier verificationCodeSupplier;

    MobileRegistrationBuilder(Store store, Keys keys, ErrorHandler errorHandler, ExecutorService socketExecutor) {
        this.store = store;
        this.keys = keys;
        this.errorHandler = errorHandler;
        this.socketExecutor = socketExecutor;
    }

    /**
     * Sets the handler that provides the verification code when verifying an account
     *
     * @param verificationCodeSupplier the non-null supplier
     * @return the same instance
     */
    @SuppressWarnings("unchecked")
    public T verificationCodeSupplier(Supplier<String> verificationCodeSupplier) {
        this.verificationCodeSupplier = AsyncVerificationCodeSupplier.of(verificationCodeSupplier);
        return (T) this;
    }

    /**
     * Sets the handler that provides the verification code when verifying an account
     *
     * @param verificationCodeSupplier the non-null supplier
     * @return the same instance
     */
    @SuppressWarnings("unchecked")
    public T verificationCodeSupplier(AsyncVerificationCodeSupplier verificationCodeSupplier) {
        this.verificationCodeSupplier = verificationCodeSupplier;
        return (T) this;
    }

    Whatsapp buildWhatsapp() {
        return this.whatsapp = Whatsapp.customBuilder()
                .store(store)
                .keys(keys)
                .errorHandler(errorHandler)
                .socketExecutor(socketExecutor)
                .build();
    }

    public final static class Unregistered extends MobileRegistrationBuilder<Unregistered> {
        private Unverified unverified;
        private VerificationCodeMethod verificationCodeMethod;

        Unregistered(Store store, Keys keys, ErrorHandler errorHandler, ExecutorService socketExecutor) {
            super(store, keys, errorHandler, socketExecutor);
            this.verificationCodeMethod = VerificationCodeMethod.SMS;
        }


        /**
         * Sets the type of method used to verify the account
         *
         * @param verificationCodeMethod the non-null method
         * @return the same instance
         */
        public Unregistered verificationCodeMethod(VerificationCodeMethod verificationCodeMethod) {
            this.verificationCodeMethod = verificationCodeMethod;
            return this;
        }

        /**
         * Registers a phone number by asking for a verification code and then sending it to Whatsapp
         *
         * @param phoneNumber a phone number(include the prefix)
         * @return a future
         */
        public CompletableFuture<Whatsapp> register(long phoneNumber) {
            if (whatsapp != null) {
                return CompletableFuture.completedFuture(whatsapp);
            }

            Objects.requireNonNull(verificationCodeSupplier, "Expected a valid verification code supplier");
            Objects.requireNonNull(verificationCodeMethod, "Expected a valid verification method");
            if (!keys.registered()) {
                var number = PhoneNumber.of(phoneNumber);
                keys.setPhoneNumber(number);
                store.setPhoneNumber(number);
                var registration = new HttpRegistration(store, keys, verificationCodeSupplier, verificationCodeMethod);
                return registration.registerPhoneNumber()
                        .thenApply(ignored -> buildWhatsapp());
            }

            return CompletableFuture.completedFuture(buildWhatsapp());
        }


        /**
         * Asks Whatsapp for a one-time-password to start the registration process
         *
         * @param phoneNumber a phone number(include the prefix)
         * @return a future
         */
        public CompletableFuture<Unverified> requestVerificationCode(long phoneNumber) {
            if(unverified != null) {
                return CompletableFuture.completedFuture(unverified);
            }

            var number = PhoneNumber.of(phoneNumber);
            keys.setPhoneNumber(number);
            store.setPhoneNumber(number);
            if (!keys.registered()) {
                var registration = new HttpRegistration(store, keys, verificationCodeSupplier, verificationCodeMethod);
                return registration.requestVerificationCode()
                        .thenApply(ignored -> this.unverified = new Unverified(store, keys, errorHandler, socketExecutor));
            }

            this.unverified = new Unverified(store, keys, errorHandler, socketExecutor);
            return CompletableFuture.completedFuture(unverified);
        }
    }

    public final static class Unverified extends MobileRegistrationBuilder<Unverified> {
        Unverified(Store store, Keys keys, ErrorHandler errorHandler, ExecutorService socketExecutor) {
            super(store, keys, errorHandler, socketExecutor);
        }

        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<Whatsapp> verify(long phoneNumber) {
            var number = PhoneNumber.of(phoneNumber);
            keys.setPhoneNumber(number);
            store.setPhoneNumber(number);
            return verify();
        }


        /**
         * Sends the verification code you already requested to Whatsapp
         *
         * @return the same instance for chaining
         */
        public CompletableFuture<Whatsapp> verify() {
            Objects.requireNonNull(store.phoneNumber(), "Missing phone number: please specify it");
            Objects.requireNonNull(verificationCodeSupplier, "Expected a valid verification code supplier");
            var registration = new HttpRegistration(store, keys, verificationCodeSupplier, VerificationCodeMethod.NONE);
            return registration.sendVerificationCode()
                    .thenApply(ignored -> buildWhatsapp());
        }
    }
}
