package com.melodi.sampahjujur.ui.screens.shared

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melodi.sampahjujur.ui.theme.PrimaryGreen
import com.melodi.sampahjujur.ui.theme.SampahJujurTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermsAndConditionsScreen(
    onBackClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Terms & Conditions",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // Last Updated
            item {
                Text(
                    text = "Last Updated: January 26, 2025",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }

            // Introduction
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Agreement to Terms",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "By accessing or using the Sampah Jujur application, you agree to be bound by these Terms and Conditions. If you disagree with any part of these terms, you may not use our service.\n\n" +
                                    "These terms apply to all users of the app, including both households and waste collectors.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Definitions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "1. Definitions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• \"Service\" refers to the Sampah Jujur mobile application and platform\n" +
                                    "• \"User\" refers to anyone who accesses the Service\n" +
                                    "• \"Household\" refers to users who request waste collection\n" +
                                    "• \"Collector\" refers to users who collect and purchase waste\n" +
                                    "• \"Content\" refers to all text, images, data uploaded to the Service",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // User Accounts
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "2. User Accounts",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Account Creation:\n" +
                                    "• You must be at least 18 years old to create an account\n" +
                                    "• You must provide accurate and complete information\n" +
                                    "• You are responsible for maintaining account security\n" +
                                    "• You must not share your account credentials\n\n" +
                                    "Account Termination:\n" +
                                    "• We reserve the right to suspend or terminate accounts that violate these terms\n" +
                                    "• You may delete your account at any time through the app settings",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Service Usage
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "3. Service Usage",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "For Households:",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Provide accurate information about waste items\n" +
                                    "• Ensure waste is prepared for collection\n" +
                                    "• Be available at scheduled pickup times\n" +
                                    "• Provide accurate pickup location information\n" +
                                    "• Do not list hazardous or prohibited materials",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "For Collectors:",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.Black
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "• Arrive on time for scheduled pickups\n" +
                                    "• Treat households and their property with respect\n" +
                                    "• Provide fair pricing for waste items\n" +
                                    "• Handle waste responsibly and legally\n" +
                                    "• Maintain valid licenses and permits required for waste collection",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Prohibited Activities
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "4. Prohibited Activities",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "You may not:\n\n" +
                                    "• Use the Service for any illegal purpose\n" +
                                    "• Impersonate another person or entity\n" +
                                    "• Upload false or misleading information\n" +
                                    "• Harass, abuse, or harm other users\n" +
                                    "• Attempt to gain unauthorized access to the Service\n" +
                                    "• List hazardous, toxic, or prohibited waste materials\n" +
                                    "• Interfere with the proper functioning of the Service\n" +
                                    "• Use automated systems to access the Service",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Payments and Transactions
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "5. Payments and Transactions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• All transactions are conducted between households and collectors\n" +
                                    "• Sampah Jujur is not responsible for payment disputes\n" +
                                    "• Pricing is determined by mutual agreement\n" +
                                    "• Platform fees may apply to certain transactions\n" +
                                    "• All transactions must be conducted through the app\n" +
                                    "• Refund policies are subject to individual agreements",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Liability and Disclaimers
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "6. Liability and Disclaimers",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Service Provided \"As Is\":\n" +
                                    "• The Service is provided without warranties of any kind\n" +
                                    "• We do not guarantee uninterrupted or error-free operation\n" +
                                    "• We are not responsible for user-generated content\n\n" +
                                    "Limitation of Liability:\n" +
                                    "• Sampah Jujur is a platform connecting users\n" +
                                    "• We are not responsible for disputes between users\n" +
                                    "• We are not liable for damages arising from Service use\n" +
                                    "• Users engage at their own risk",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Intellectual Property
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "7. Intellectual Property",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• All app content, design, and trademarks are owned by Sampah Jujur\n" +
                                    "• You may not copy, modify, or distribute app content\n" +
                                    "• You retain ownership of content you upload\n" +
                                    "• You grant us a license to use your content for Service operation\n" +
                                    "• We may use feedback you provide without compensation",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Dispute Resolution
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "8. Dispute Resolution",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Users are encouraged to resolve disputes directly\n" +
                                    "• Contact our support team for mediation assistance\n" +
                                    "• Disputes not resolved through mediation shall be governed by Indonesian law\n" +
                                    "• Jurisdiction for legal matters is Yogyakarta, Indonesia",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Changes to Terms
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "9. Changes to Terms",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "We reserve the right to modify these Terms and Conditions at any time. Changes will be effective immediately upon posting in the app. Your continued use of the Service after changes constitutes acceptance of the modified terms.\n\n" +
                                    "We will notify users of significant changes via in-app notification or email.",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Contact Information
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "10. Contact Information",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryGreen
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "For questions regarding these Terms and Conditions:\n\n" +
                                    "Email: legal@sampahjujur.com\n" +
                                    "Phone: +62 123 4567 890\n" +
                                    "Address: Jl. Lingkungan Hijau No. 123, Yogyakarta, Indonesia\n\n" +
                                    "Business Hours: Monday - Friday, 9:00 AM - 5:00 PM WIB",
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            // Acknowledgment
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = PrimaryGreen.copy(alpha = 0.1f)
                    ),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "By using Sampah Jujur, you acknowledge that you have read, understood, and agree to be bound by these Terms and Conditions.",
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TermsAndConditionsScreenPreview() {
    SampahJujurTheme {
        TermsAndConditionsScreen()
    }
}
