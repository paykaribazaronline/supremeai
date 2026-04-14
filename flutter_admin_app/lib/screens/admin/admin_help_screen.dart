import 'package:flutter/material.dart';

/// Help tips widget that shows beginner-friendly tips for the admin dashboard
class AdminHelpPanel extends StatefulWidget {
  final String title;
  final List<String> tips;
  final List<String>? steps;
  final List<String>? warnings;
  final String? difficulty; // 'easy', 'medium', 'hard'

  const AdminHelpPanel({
    Key? key,
    required this.title,
    required this.tips,
    this.steps,
    this.warnings,
    this.difficulty = 'medium',
  }) : super(key: key);

  @override
  State<AdminHelpPanel> createState() => _AdminHelpPanelState();
}

class _AdminHelpPanelState extends State<AdminHelpPanel> {
  bool _expanded = false;

  Color _getDifficultyColor() {
    switch (widget.difficulty) {
      case 'easy':
        return Colors.green;
      case 'hard':
        return Colors.red;
      default:
        return Colors.blue;
    }
  }

  String _getDifficultyLabel() {
    switch (widget.difficulty) {
      case 'easy':
        return 'সহজ';
      case 'hard':
        return 'কঠিন';
      default:
        return 'মাঝারি';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 2,
      margin: const EdgeInsets.symmetric(vertical: 8, horizontal: 0),
      child: ExpansionTile(
        tilePadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        title: Row(
          children: [
            const Icon(Icons.lightbulb_outline, color: Colors.amber),
            const SizedBox(width: 12),
            Expanded(
              child: Text(
                widget.title,
                style: const TextStyle(fontWeight: FontWeight.bold),
              ),
            ),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: _getDifficultyColor(),
                borderRadius: BorderRadius.circular(4),
              ),
              child: Text(
                _getDifficultyLabel(),
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 12,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ),
          ],
        ),
        onExpansionChanged: (expanded) {
          setState(() => _expanded = expanded);
        },
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Tips section
                if (widget.tips.isNotEmpty) ...[
                  const Text(
                    'টিপস:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  ...widget.tips.map((tip) => Padding(
                    padding: const EdgeInsets.only(bottom: 6),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text('• '),
                        Expanded(child: Text(tip)),
                      ],
                    ),
                  )),
                  const SizedBox(height: 16),
                ],

                // Steps section
                if (widget.steps != null && widget.steps!.isNotEmpty) ...[
                  const Text(
                    'ধাপসমূহ:',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  ...List.generate(widget.steps!.length, (index) {
                    return Padding(
                      padding: const EdgeInsets.only(bottom: 6),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Container(
                            width: 24,
                            height: 24,
                            decoration: BoxDecoration(
                              color: Colors.blue,
                              borderRadius: BorderRadius.circular(12),
                            ),
                            child: Center(
                              child: Text(
                                '${index + 1}',
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.bold,
                                  fontSize: 12,
                                ),
                              ),
                            ),
                          ),
                          const SizedBox(width: 12),
                          Expanded(child: Text(widget.steps![index])),
                        ],
                      ),
                    );
                  }),
                  const SizedBox(height: 16),
                ],

                // Warnings section
                if (widget.warnings != null && widget.warnings!.isNotEmpty) ...[
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.red.shade50,
                      border: Border.all(color: Colors.red.shade300),
                      borderRadius: BorderRadius.circular(4),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            Icon(Icons.warning, color: Colors.red.shade700),
                            const SizedBox(width: 8),
                            Text(
                              'সতর্কতা:',
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                color: Colors.red.shade700,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        ...widget.warnings!.map((warning) => Padding(
                          padding: const EdgeInsets.only(bottom: 4),
                          child: Row(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text('⚠️ '),
                              Expanded(child: Text(warning)),
                            ],
                          ),
                        )),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

/// Main help tips screen for the Flutter admin app
class AdminHelpScreen extends StatelessWidget {
  const AdminHelpScreen({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('অ্যাডমিন প্যানেল সাহায্য'),
        elevation: 0,
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Welcome message
          Card(
            color: Colors.blue.shade50,
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'স্বাগতম অ্যাডমিন প্যানেলে! 👋',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                  ),
                  const SizedBox(height: 8),
                  const Text(
                    'এখানে আপনি আপনার SupremeAI সিস্টেম পরিচালনা করতে পারেন। নিচের টিপসগুলি আপনাকে প্রতিটি ফিচার বুঝতে সাহায্য করবে।',
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: ElevatedButton.icon(
                          icon: const Icon(Icons.book),
                          label: const Text('স্টার্ট করুন'),
                          onPressed: () {
                            ScaffoldMessenger.of(context).showSnackBar(
                              const SnackBar(
                                content: Text(
                                  'ধাপ ১: নিয়ন্ত্রক মোড বুঝুন\nধাপ ২: নিরাপদে পরিবর্তন করুন\nধাপ ৩: সিস্টেম মনিটর করুন',
                                ),
                              ),
                            );
                          },
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),

          // Control Modes
          const Text(
            'নিয়ন্ত্রক মোডসমূহ',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),

          const AdminHelpPanel(
            title: 'AUTO মোড - দ্রুত সম্পাদন',
            difficulty: 'hard',
            tips: [
              'সবকিছু তাৎক্ষণিকভাবে ঘটে',
              'অভিজ্ঞ ইউজারদের জন্য',
              'আপনি যা করেন তা অবিলম্বে কার্যকর হয়',
            ],
            steps: [
              'AUTO মোড ক্লিক করুন',
              'পরিবর্তন করুন',
              'এটি তাৎক্ষণিকভাবে ঘটবে',
            ],
            warnings: [
              'নতুনদের জন্য সুপারিশ করা হয় না',
              'প্রথম সপ্তাহে WAIT মোড ব্যবহার করুন',
            ],
          ),

          const AdminHelpPanel(
            title: 'WAIT মোড - নিরাপদ অনুমোদন',
            difficulty: 'easy',
            tips: [
              'পরিবর্তনগুলি অনুমোদনের জন্য অপেক্ষা করে',
              'নতুনদের জন্য সেরা বেছে নুন',
              'আপনি প্রতিটি অ্যাকশন দেখে অনুমোদন দিন',
            ],
            steps: [
              'WAIT মোড ক্লিক করুন',
              'পরিবর্তন করুন',
              'Pending Actions এ দেখুন',
              'সাবধানে পড়ুন এবং অনুমোদন করুন',
            ],
          ),

          const AdminHelpPanel(
            title: 'STOP মোড - জরুরি বিরতি',
            difficulty: 'easy',
            tips: [
              'সবকিছু অবিলম্বে থেমে যায়',
              'জরুরী অবস্থায় ব্যবহার করুন',
              'এটি কখনও ভুল পছন্দ নয়',
            ],
            warnings: [
              'কিছু ভুল লাগলে সাথে সাথে STOP করুন',
              'সিস্টেম সম্পূর্ণ হ্যালট হবে',
            ],
          ),

          const SizedBox(height: 24),
          const Text(
            'সাধারণ কাজ',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),

          const AdminHelpPanel(
            title: 'API কী পরিচালনা করুন',
            difficulty: 'medium',
            tips: [
              'API কীগুলি OpenAI, Google ইত্যাদি থেকে আসে',
              'এগুলি গোপনীয় - সবার সাথে শেয়ার করবেন না',
              'নিয়মিত আপডেট রাখুন',
            ],
            steps: [
              'API Keys সেকশন খুলুন',
              'Add New Key ক্লিক করুন',
              'প্রদানকারী নির্বাচন করুন',
              'আপনার কী পেস্ট করুন এবং সংরক্ষণ করুন',
            ],
          ),

          const AdminHelpPanel(
            title: 'সিস্টেম স্বাস্থ্য পরীক্ষা করুন',
            difficulty: 'easy',
            tips: [
              '🟢 সবুজ = সবকিছু ভালো',
              '🟡 হলুদ = কিছু সমস্যা অিছে',
              '🔴 লাল = তাৎক্ষণিক পদক্ষেপ প্রয়োজন',
            ],
            steps: [
              'ড্যাশবোর্ড হোম খুলুন',
              'সিস্টেম স্বাস্থ্য দেখুন',
              'লক্ষণ পড়ুন এবং পদক্ষেপ নিন',
            ],
          ),

          const AdminHelpPanel(
            title: 'অডিট লগ পড়ুন',
            difficulty: 'medium',
            tips: [
              'প্রতিটি অ্যাকশন রেকর্ড করা হয়',
              'কি ঘটেছে তা দেখতে প্রথমে এটি চেক করুন',
              'এটি আপনার নিরাপত্তা প্রমাণ',
            ],
            steps: [
              'Audit Trail স্ক্রল করুন',
              'এন্ট্রি পড়ুন',
              'কিভাবে এবং কখন সবকিছু ঘটেছে বুঝুন',
            ],
          ),

          const SizedBox(height: 24),
          const Text(
            'সমস্যা সমাধান',
            style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 12),

          const AdminHelpPanel(
            title: 'কিছু ভুল দেখলে কি করবেন',
            difficulty: 'easy',
            tips: [
              'প্রথমে ঘাবড়াবেন না',
              'সাথে সাথে STOP ক্লিক করুন',
              'Audit Trail চেক করুন কি ঘটেছে',
            ],
            steps: [
              'STOP মোড ক্লিক করুন',
              'Audit Trail দেখুন',
              'কি ঘটেছে বুঝুন',
              'প্রয়োজনে সংশোধন করুন',
            ],
            warnings: [
              'কখনও আতঙ্কিত সিদ্ধান্ত নেবেন না',
              'সবসময় STOP সেফ বিকল্প',
            ],
          ),

          const SizedBox(height: 24),
          Card(
            color: Colors.green.shade50,
            child: const Padding(
              padding: EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(Icons.check_circle, color: Colors.green),
                      SizedBox(width: 8),
                      Text(
                        'আপনি এখন প্রস্তুত! 🎉',
                        style: TextStyle(fontWeight: FontWeight.bold),
                      ),
                    ],
                  ),
                  SizedBox(height: 8),
                  Text(
                    'এক সপ্তাহ WAIT মোডে অভিজ্ঞতা নিয়ে আত্মবিশ্বাসী হয়ে উঠুন। প্রতিটি ধাপে আপনি শিখছেন এবং শক্তিশালী হচ্ছেন।',
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 40),
        ],
      ),
    );
  }
}
