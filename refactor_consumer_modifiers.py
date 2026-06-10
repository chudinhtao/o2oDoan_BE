import re

file_path = r'd:\srcDOAN\backend\inventory-service\src\main\java\com\fnb\inventory\service\OrderEventConsumerService.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Add modifier logic inside handleOrderCreatedOrPaid
old_created_loop = '''for (JsonNode item : event.get("lineItems")) {
                    UUID orderLineItemId = UUID.fromString(item.get("orderLineItemId").asText());
                    UUID menuItemId = UUID.fromString(item.get("menuItemId").asText());
                    int quantity = item.get("quantity").asInt();

                    processItemDeduction(orderId, orderLineItemId, menuItemId, quantity);
                }'''

new_created_loop = '''for (JsonNode item : event.get("lineItems")) {
                    UUID orderLineItemId = UUID.fromString(item.get("orderLineItemId").asText());
                    UUID menuItemId = UUID.fromString(item.get("menuItemId").asText());
                    int quantity = item.get("quantity").asInt();

                    // Trừ kho cho món chính (Main Item)
                    processItemDeduction(orderId, orderLineItemId, menuItemId, quantity);
                    
                    // Trừ kho cho Topping / Custom (Modifiers)
                    if (item.has("modifiers")) {
                        for (JsonNode modifier : item.get("modifiers")) {
                            if (modifier.has("menuItemId")) {
                                UUID modMenuItemId = UUID.fromString(modifier.get("menuItemId").asText());
                                int modQty = modifier.has("quantity") ? modifier.get("quantity").asInt() : 1;
                                // Nhân số lượng topping với số lượng món chính
                                processItemDeduction(orderId, orderLineItemId, modMenuItemId, quantity * modQty);
                            }
                        }
                    }
                }'''
content = content.replace(old_created_loop, new_created_loop)

# Add modifier logic inside handleOrderCancelled
old_cancel_loop = '''for (JsonNode item : event.get("lineItems")) {
                    UUID orderLineItemId = UUID.fromString(item.get("orderLineItemId").asText());
                    UUID menuItemId = UUID.fromString(item.get("menuItemId").asText());
                    int quantity = item.get("quantity").asInt();

                    processItemCancel(orderId, orderLineItemId, menuItemId, quantity, isWaste);
                }'''

new_cancel_loop = '''for (JsonNode item : event.get("lineItems")) {
                    UUID orderLineItemId = UUID.fromString(item.get("orderLineItemId").asText());
                    UUID menuItemId = UUID.fromString(item.get("menuItemId").asText());
                    int quantity = item.get("quantity").asInt();

                    // Hoàn kho/Hao hụt cho món chính
                    processItemCancel(orderId, orderLineItemId, menuItemId, quantity, isWaste);
                    
                    // Hoàn kho/Hao hụt cho Topping / Custom (Modifiers)
                    if (item.has("modifiers")) {
                        for (JsonNode modifier : item.get("modifiers")) {
                            if (modifier.has("menuItemId")) {
                                UUID modMenuItemId = UUID.fromString(modifier.get("menuItemId").asText());
                                int modQty = modifier.has("quantity") ? modifier.get("quantity").asInt() : 1;
                                processItemCancel(orderId, orderLineItemId, modMenuItemId, quantity * modQty, isWaste);
                            }
                        }
                    }
                }'''
content = content.replace(old_cancel_loop, new_cancel_loop)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Added modifier support successfully!")
