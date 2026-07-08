# Báo cáo dự án Android Studio - V.League Ticket

Tài liệu này là bản mô tả tổng quan dự án `V.League Ticket`, có thể dùng làm khung báo cáo final hoặc tài liệu README trên GitHub. Nội dung được cập nhật theo trạng thái hiện tại của app sau các phần tối ưu giao diện, admin, thông báo, đơn hàng, vé và dữ liệu legacy.

## 1. Giới thiệu dự án

`V.League Ticket` là ứng dụng Android phục vụ người hâm mộ bóng đá Việt Nam. Ứng dụng kết hợp ba nhóm chức năng chính:

- Tra cứu thông tin V.League: lịch thi đấu, kết quả, bảng xếp hạng, tin tức, chi tiết trận và hồ sơ câu lạc bộ.
- Thương mại điện tử mini: shop sản phẩm CLB, giỏ hàng, thanh toán, theo dõi đơn hàng.
- Đặt vé và quản lý người dùng: chọn trận, giữ ghế, tạo đơn vé, lịch sử vé, thông báo và khu vực admin.

Dự án được xây dựng bằng Java/XML trên Android Studio, tổ chức theo nhiều `Fragment`, dùng Firebase cho xác thực/lưu trữ và gọi API bên ngoài để lấy dữ liệu bóng đá.

## 2. Mục tiêu dự án

- Xây dựng app Android có luồng người dùng tương đối đầy đủ: xem thông tin, đăng nhập, mua hàng, đặt vé, nhận thông báo và xem lịch sử.
- Tích hợp dữ liệu động từ API/Firebase thay vì chỉ dùng dữ liệu tĩnh.
- Có khu vực admin để quản lý đơn hàng, vận chuyển, sản phẩm, banner, thông báo và người dùng.
- Tối ưu trải nghiệm danh sách bằng `RecyclerView`, `ListAdapter`, `DiffUtil`, cache ảnh bằng Glide và hạn chế xử lý dư thừa trên UI.
- Thiết kế giao diện theo hướng app thương mại thực tế hơn, có bottom navigation, màn xác nhận đơn, dashboard admin và hồ sơ CLB rõ ràng.

## 3. Công nghệ sử dụng

- Android Studio, Java, XML.
- Fragment-based navigation.
- ViewBinding.
- RecyclerView, ListAdapter, DiffUtil.
- ConstraintLayout, Material Components, CardView.
- Retrofit, Gson, OkHttp Logging Interceptor.
- Glide để tải ảnh tin tức, sản phẩm, logo CLB, cầu thủ.
- Firebase Authentication.
- Firebase Firestore.
- Media3 ExoPlayer cho video intro/background.

Cấu hình chính:

- `compileSdk 36`
- `minSdk 26`
- `targetSdk 36`
- Java 11
- ViewBinding bật trong `app/build.gradle`

## 4. Kiến trúc tổng quan

Dự án được chia theo nhóm package chính:

- `data`: gọi API, đồng bộ Firebase, backup dữ liệu web.
- `domain`: model dữ liệu dùng chung như fixture, standing, news, season.
- `presentation`: các màn hình UI, adapter, store và luồng nghiệp vụ người dùng.
- `di`: container dùng chung cho app.

Điểm vào app là `MainActivity`. Các màn hình chính được điều hướng bằng fragment transaction trong cùng một activity.

## 5. Các file đại diện

Khởi động và cấu hình:

- `app/src/main/java/com/finalproject/v_league_ticket/MainActivity.java`
- `app/src/main/java/com/finalproject/v_league_ticket/di/AppContainer.java`
- `app/src/main/AndroidManifest.xml`
- `app/build.gradle`

Dữ liệu và đồng bộ:

- `app/src/main/java/com/finalproject/v_league_ticket/data/remote/VLeagueApiClient.java`
- `app/src/main/java/com/finalproject/v_league_ticket/data/remote/VietnamAddressApiClient.java`
- `app/src/main/java/com/finalproject/v_league_ticket/data/firebase/FirebaseSchemaSeeder.java`
- `app/src/main/java/com/finalproject/v_league_ticket/data/firebase/FirebaseWebBackup.java`

Màn hình chính:

- `presentation/homepage/HomepageFragment.java`
- `presentation/matches/FixturesFragment.java`
- `presentation/matches/ResultsFragment.java`
- `presentation/matches/StandingsFragment.java`
- `presentation/matches/MatchCenterFragment.java`
- `presentation/news/NewsFragment.java`
- `presentation/shop/ShopFragment.java`
- `presentation/booking/TicketBookingFragment.java`
- `presentation/profile/ProfileFragment.java`
- `presentation/admin/AdminDashboardFragment.java`

Layout quan trọng:

- `res/layout/fragment_homepage.xml`
- `res/layout/fragment_matches_fixtures.xml`
- `res/layout/fragment_matches_results.xml`
- `res/layout/fragment_ticket_booking.xml`
- `res/layout/fragment_shop.xml`
- `res/layout/fragment_checkout.xml`
- `res/layout/fragment_order_success.xml`
- `res/layout/fragment_profile.xml`
- `res/layout/fragment_admin_dashboard.xml`
- `res/layout/fragment_admin_module_detail.xml`

Component dùng lại:

- `res/layout/component_app_header.xml`
- `res/layout/component_bottom_nav_shop.xml`
- `res/layout/component_bottom_nav_news.xml`
- `res/layout/component_bottom_nav_matches.xml`
- `res/layout/component_bottom_nav_profile.xml`
- `res/layout/item_admin_module_card.xml`
- `res/layout/item_admin_document.xml`

## 6. Luồng hoạt động chính

1. Người dùng mở app, `MainActivity` khởi tạo Firebase schema và hiển thị `HomepageFragment`.
2. Trang chủ tải trận nổi bật, lịch sắp tới và tin tức thông qua `VLeagueApiClient`.
3. Người dùng chuyển qua các tab: Trang chủ, Trận đấu, Shop, Tin tức, Cá nhân.
4. Nếu cần tài khoản, app chuyển sang `AuthLoginFragment` hoặc `AuthRegisterFragment`.
5. Khi mua hàng hoặc đặt vé, app tạo đơn trong Firestore với trạng thái ban đầu `pending_payment`.
6. Admin xác nhận thanh toán bằng module đơn hàng.
7. Với đơn shop đã xác nhận, module vận chuyển xử lý mã SPX, bàn giao, hoàn tất và gửi thông báo cho user.
8. Người dùng xem lịch sử đơn/vé/thông báo trong khu vực cá nhân.

## 7. Chức năng người dùng

### 7.1 Trang chủ

Trang chủ hiển thị:

- Trận nổi bật.
- Lịch thi đấu sắp tới.
- Tin mới VPF.
- Quick actions.
- Header có icon thông báo và badge đỏ khi còn thông báo chưa đọc.
- Bottom navigation dạng dock bám đáy, dùng chung ở các tab chính.

Các file chính:

- `HomepageFragment.java`
- `fragment_homepage.xml`
- `HomeFixtureAdapter.java`
- `NewsAdapter.java`

### 7.2 Trận đấu

Nhóm trận đấu gồm:

- Lịch thi đấu.
- Kết quả.
- Bảng xếp hạng.
- Chi tiết trận.
- Thống kê, đội hình, diễn biến và đối đầu.

Các điểm đã tối ưu:

- RecyclerView trong các màn nằm trong `NestedScrollView` được chỉnh `setHasFixedSize(false)` để tránh lỗi không hiển thị/đo sai chiều cao.
- Bộ lọc kết quả sắp xếp vòng từ vòng mới nhất xuống vòng 1 để phù hợp thói quen xem trận vừa diễn ra.

File chính:

- `FixturesFragment.java`
- `ResultsFragment.java`
- `StandingsFragment.java`
- `MatchCenterFragment.java`
- `UpcomingMatchDetailFragment.java`
- `MatchAdapter.java`
- `StandingsAdapter.java`

### 7.3 Tin tức

Màn hình tin tức hỗ trợ:

- Danh sách tin từ VPF.
- Lọc theo danh mục.
- Tìm kiếm tin tức.
- Nhãn danh mục tiếng Việt có dấu như `Hạng Nhất`, `Cúp Quốc gia`, `Bóng đá Việt Nam`.

File chính:

- `NewsFragment.java`
- `NewsCategory.java`
- `NewsCategoryAdapter.java`
- `NewsListAdapter.java`
- `item_news_card.xml`
- `item_news_category_chip.xml`

### 7.4 Hồ sơ câu lạc bộ

Hồ sơ CLB có:

- Thông tin tổng quan.
- Thống kê.
- Danh sách cầu thủ.
- Cửa hàng sản phẩm theo CLB.

Phần cầu thủ đã được nâng cấp:

- Hiển thị dạng grid 2 cột.
- Có ảnh cầu thủ nếu API cung cấp.
- Có số áo, vị trí thi đấu và quốc tịch.
- Có cơ chế loại cầu thủ trùng dựa trên player id hoặc tên/số áo.

File chính:

- `ClubProfileFragment.java`
- `ClubProfileDirectory.java`
- `VLeagueApiClient.java`
- `fragment_club_profile.xml`

### 7.5 Đặt vé

Luồng đặt vé gồm:

- Chọn trận.
- Chọn sân/khu vực.
- Chọn ghế.
- Giữ ghế tạm thời.
- Tạo đơn vé.
- Ghi lịch sử vé để người dùng xem trong mục “Vé của tôi”.

Các điểm xử lý dữ liệu:

- Đơn vé được lưu theo type `ticket`.
- Lịch sử vé có cơ chế seed từ `bookings` và `ticket_orders` sang `user_ticket_history` để tránh tình trạng số lượng vé và danh sách vé không khớp.
- Vé được tách khỏi luồng vận chuyển shop để không bị xử lý như hàng hóa giao SPX.

File chính:

- `TicketBookingFragment.java`
- `StadiumSeatMapView.java`
- `BookingMatch.java`
- `BookingVenue.java`
- `TicketSeat.java`
- `fragment_ticket_booking.xml`

### 7.6 Shop, giỏ hàng và thanh toán

Shop hỗ trợ:

- Xem sản phẩm theo CLB.
- Xem chi tiết sản phẩm.
- Thêm vào giỏ.
- Cập nhật số lượng.
- Thanh toán.
- Tạo đơn hàng trong Firestore.
- Màn xác nhận đơn sau checkout.

Màn xác nhận đơn đã được làm lại:

- Bố cục xác nhận riêng, không còn card hero cũ.
- Có icon xác nhận, trạng thái, mã đơn và timeline xử lý.
- Hai nút hành động dưới màn hình: xem đơn và về trang chủ.

File chính:

- `ShopFragment.java`
- `ShopProductAdapter.java`
- `ShopClubAdapter.java`
- `ProductDetailFragment.java`
- `CartFragment.java`
- `CartItemAdapter.java`
- `CheckoutFragment.java`
- `OrderSuccessFragment.java`
- `fragment_order_success.xml`

## 8. Hồ sơ, lịch sử và thông báo

Khu vực cá nhân gồm:

- Thông tin tài khoản.
- Lịch sử đơn hàng.
- Vé của tôi.
- Huy hiệu/điểm thành viên.
- Thông báo.
- Đổi thông tin cá nhân và đổi mật khẩu.

Thông báo đã được nâng cấp:

- Khi vào màn thông báo, các thông báo chưa đọc được đánh dấu đã đọc.
- Nhãn “Mới” biến mất sau khi đọc.
- Badge đỏ ở icon thông báo ngoài header biến mất khi không còn thông báo chưa đọc.
- Hỗ trợ thông báo broadcast và thông báo theo user.

File chính:

- `ProfileFragment.java`
- `ProfileHistoryFragment.java`
- `ProfileHistoryAdapter.java`
- `NotificationReadStore.java`
- `HeaderNotificationRouter.java`
- `OrderDetailFragment.java`
- `UserEngagementManager.java`

## 9. Khu vực admin

Admin dashboard gồm các module:

- Người dùng.
- Đơn hàng.
- Sản phẩm.
- Vận chuyển.
- CLB thiếu sản phẩm.
- Banner.
- Thông báo.
- Cài đặt.

Các cải tiến quan trọng:

- Card dashboard admin được căn lại đều 2 cột.
- `AdminModuleDetailFragment` dùng `RecyclerView + ListAdapter + DiffUtil`, thay cho cách thêm view thủ công.
- Danh sách admin có phân trang cơ bản với `ADMIN_PAGE_SIZE = 30`.
- Có tìm kiếm trong module admin.
- Nội dung admin đã sửa lỗi hiển thị `\n` literal, chuyển sang xuống dòng thật.
- Module thông báo admin hiển thị nội dung dễ đọc hơn và nút xóa rõ ràng hơn.

File chính:

- `AdminDashboardFragment.java`
- `AdminModuleDetailFragment.java`
- `AdminSummaryStore.java`
- `fragment_admin_dashboard.xml`
- `fragment_admin_module_detail.xml`
- `item_admin_module_card.xml`
- `item_admin_document.xml`

## 10. Luồng đơn hàng và vận chuyển

Luồng đơn hàng hiện tại:

1. User đặt hàng trong shop.
2. App tạo document trong `orders` với trạng thái `pending_payment`.
3. Admin vào module đơn hàng để xác nhận thanh toán, đổi trạng thái sang `confirmed`.
4. Đơn shop có trạng thái `confirmed` hoặc `shipping` được xem là đơn cần xử lý vận chuyển.
5. Admin nhập mã vận đơn SPX.
6. Admin bàn giao SPX, trạng thái chuyển `shipping`.
7. Khi giao xong, admin chuyển `completed`.
8. User nhận thông báo cập nhật trạng thái.

Module vận chuyển chỉ xử lý đơn shop, không xử lý vé. Đây là cách tách luồng đúng hơn trong mô hình thương mại điện tử: vé là dịch vụ điện tử/soát vé, còn shop là hàng hóa cần giao.

File chính:

- `AdminModuleDetailFragment.java`
- `AdminDashboardFragment.java`
- `AdminSummaryStore.java`
- `OrderDetailFragment.java`
- `UserEngagementManager.java`

## 11. Xử lý dữ liệu cũ và ổn định app

Dự án có một số lớp xử lý dữ liệu legacy để tránh lỗi do dữ liệu cũ sinh ra từ code trước:

- Chuẩn hóa trạng thái cũ như `paid_mock`, `mock_paid`, `mock_payment`, `paid` về `pending_payment`.
- Xóa/ẩn các đơn shop không có sản phẩm thật để tránh hiển thị đơn rác.
- Không xóa đơn vé khi cleanup đơn shop.
- Đồng bộ lịch sử vé từ nhiều collection cũ để danh sách vé khớp với số lượng hiển thị.

File chính:

- `LegacyFirestoreCleanup.java`
- `ProfileHistoryFragment.java`
- `OrderDetailFragment.java`

## 12. Tối ưu hiệu năng và trải nghiệm

Các điểm tối ưu đã có trong dự án:

- Dùng `ListAdapter + DiffUtil` ở nhiều danh sách để giảm redraw không cần thiết.
- Dùng Glide để cache và tải ảnh bất đồng bộ.
- Dùng RecyclerView thay vì thêm view thủ công ở admin module.
- Phân trang danh sách admin để tránh đọc quá nhiều document một lần.
- Tắt item animator ở danh sách admin để giảm giật khi cập nhật.
- Tách summary admin vào `AdminSummaryStore` để tránh dashboard luôn phải đọc toàn bộ dữ liệu nếu có summary hợp lệ.
- Bottom navigation được chỉnh thành dock bám đáy, tránh giao diện nổi lơ lửng.
- Các màn danh sách trong `NestedScrollView` được chỉnh đo chiều cao phù hợp để tránh lỗi trống dữ liệu.

## 13. Firebase và dữ liệu

Firebase được dùng cho:

- Authentication: đăng nhập, đăng ký, phân quyền admin/user.
- Firestore: users, orders, bookings, ticket_orders, user_ticket_history, notifications, shop_products, app_banners, app_settings, admin_summaries.
- Schema seeding: tạo dữ liệu/cấu trúc ban đầu nếu thiếu.
- Backup/cache dữ liệu web qua `FirebaseWebBackup`.

Một số collection quan trọng:

- `users`
- `orders`
- `bookings`
- `ticket_orders`
- `user_ticket_history`
- `notifications`
- `shop_products`
- `admin_summaries`
- `app_banners`
- `app_settings`

## 14. Ưu điểm

- Có đủ cả phía user và admin.
- Có dữ liệu thể thao động, không chỉ là app tĩnh.
- Có luồng thương mại điện tử mini: shop, giỏ hàng, checkout, đơn hàng, vận chuyển.
- Có luồng đặt vé riêng với chọn ghế và lịch sử vé.
- Có thông báo và trạng thái đọc/chưa đọc.
- Giao diện nhiều phần đã được chỉnh theo hướng app thực tế hơn.
- Cấu trúc file rõ ràng, dễ trình bày trong báo cáo và demo.

## 15. Hạn chế hiện tại

- Thanh toán vẫn là mô phỏng/chờ admin xác nhận, chưa tích hợp cổng thanh toán thật.
- Chưa có push notification thật qua FCM, hiện mới là thông báo lưu trong Firestore.
- Dashboard doanh thu chưa có biểu đồ theo ngày/tuần/tháng.
- Chưa có quản lý SKU/biến thể/kho realtime cho sản phẩm.
- Module vận chuyển đang mô phỏng SPX ở mức quản lý mã vận đơn/trạng thái, chưa gọi API hãng vận chuyển thật.
- Một số dữ liệu phụ thuộc API bên ngoài nên cần mạng để hiển thị đầy đủ.
- Chưa có test tự động đầy đủ cho toàn bộ luồng đặt vé/shop/admin.

## 16. Hướng phát triển tiếp theo

- Tích hợp cổng thanh toán thật như MoMo/VNPay/Stripe tùy yêu cầu.
- Tích hợp FCM để gửi push notification thật.
- Bổ sung dashboard doanh thu theo ngày, tuần, tháng.
- Thêm quản lý tồn kho, SKU, biến thể size/màu cho sản phẩm.
- Tối ưu module vận chuyển bằng API đơn vị giao hàng thật.
- Bổ sung pagination sâu hơn cho các danh sách người dùng/đơn hàng/sản phẩm.
- Viết test cho các luồng quan trọng: checkout, đặt vé, xác nhận đơn, thông báo đã đọc.

## 17. Kết luận

`V.League Ticket` là một ứng dụng Android đa chức năng cho bóng đá Việt Nam, kết hợp tra cứu thông tin giải đấu, đặt vé, mua sắm, quản lý tài khoản và quản trị hệ thống. Dự án phù hợp để làm báo cáo final vì có cả UI, dữ liệu động, Firebase, workflow nhiều bước và module admin.

Câu tóm tắt khi bảo vệ:

> Dự án là ứng dụng Android cho người hâm mộ V.League, hỗ trợ xem thông tin giải đấu, đặt vé, mua sản phẩm CLB, theo dõi đơn hàng/thông báo và cung cấp khu vực admin để quản lý vận hành.
