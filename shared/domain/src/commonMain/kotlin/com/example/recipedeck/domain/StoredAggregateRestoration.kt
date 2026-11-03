package com.example.recipedeck.domain

@RequiresOptIn(
    message = "保存先から集約を読み戻すときだけ使う。集約を作る・変えるときは、集約の操作を使う",
    level = RequiresOptIn.Level.ERROR,
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class StoredAggregateRestoration
