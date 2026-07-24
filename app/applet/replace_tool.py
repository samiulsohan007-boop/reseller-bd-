with open('app/src/main/java/com/example/ui/screens/MainAppScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

target = 'Text("Payment Status: ${order.paymentStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF58220))'
replacement = """Text("Payment Status: ${order.paymentStatus}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF58220))

                                    if (order.productInfo.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        ) {
                                            Column(modifier = Modifier.padding(10.dp)) {
                                                Text("Ordered Products:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(order.productInfo, fontSize = 11.sp, lineHeight = 14.sp)
                                            }
                                        }
                                    }"""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/ui/screens/MainAppScreen.kt', 'w', encoding='utf-8') as f:
        f.write(content)
    print('SUCCESS')
else:
    print('TARGET NOT FOUND')
