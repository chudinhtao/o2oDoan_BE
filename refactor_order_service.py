import re

# 1. Update OrderItemOption.java
option_file = r'd:\srcDOAN\backend\order-service\src\main\java\com\fnb\order\entity\OrderItemOption.java'
with open(option_file, 'r', encoding='utf-8') as f:
    opt_content = f.read()

if 'UUID menuItemId' not in opt_content:
    opt_content = opt_content.replace(
        'private String optionName;',
        'private String optionName;\n\n    @Column(name = "menu_item_id")\n    private UUID menuItemId;'
    )
    with open(option_file, 'w', encoding='utf-8') as f:
        f.write(opt_content)

# 2. Update OrderPaidEvent.java
paid_event_file = r'd:\srcDOAN\backend\order-service\src\main\java\com\fnb\order\dto\event\OrderPaidEvent.java'
with open(paid_event_file, 'r', encoding='utf-8') as f:
    paid_content = f.read()

if 'String kitchenStatus' not in paid_content:
    old_line_item = '''public static class LineItem {
        private UUID orderLineItemId;
        private UUID menuItemId; // This is the saleItemId for Recipe lookup
        private Integer quantity;
    }'''
    new_line_item = '''public static class LineItem {
        private UUID orderLineItemId;
        private UUID menuItemId; // This is the saleItemId for Recipe lookup
        private Integer quantity;
        private String kitchenStatus;
        private java.util.List<Modifier> modifiers;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Modifier {
        private UUID menuItemId;
        private String optionName;
    }'''
    paid_content = paid_content.replace(old_line_item, new_line_item)
    with open(paid_event_file, 'w', encoding='utf-8') as f:
        f.write(paid_content)

# 3. Update OrderService.java
order_service_file = r'd:\srcDOAN\backend\order-service\src\main\java\com\fnb\order\service\OrderService.java'
with open(order_service_file, 'r', encoding='utf-8') as f:
    service_content = f.read()

# Replace mapping for paidEvent
old_mapping = '''.map(i -> OrderPaidEvent.LineItem.builder()
                          .orderLineItemId(i.getId())
                          .menuItemId(i.getMenuItemId())
                          .quantity(i.getQuantity())
                          .build())'''

new_mapping = '''.map(i -> OrderPaidEvent.LineItem.builder()
                          .orderLineItemId(i.getId())
                          .menuItemId(i.getMenuItemId())
                          .quantity(i.getQuantity())
                          .kitchenStatus(i.getStatus())
                          .modifiers(i.getOptions() != null ? i.getOptions().stream()
                                  .map(opt -> OrderPaidEvent.Modifier.builder()
                                          .menuItemId(opt.getMenuItemId())
                                          .optionName(opt.getOptionName())
                                          .build())
                                  .toList() : java.util.Collections.emptyList())
                          .build())'''

service_content = service_content.replace(old_mapping, new_mapping)

with open(order_service_file, 'w', encoding='utf-8') as f:
    f.write(service_content)

print("Order Service Refactored!")
