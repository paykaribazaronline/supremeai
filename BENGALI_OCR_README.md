# Bengali OCR Feature for SupremeAI

This feature adds powerful Bengali text recognition and Excel conversion capabilities to your SupremeAI platform.

## 🚀 Features

- **Accurate Bengali OCR** using Google Cloud Vision API
- **Batch Processing** of multiple images
- **Table Structure Detection** for tabular data
- **Excel Export** with structured formatting
- **Firebase Integration** for seamless workflow
- **Admin Dashboard** integration

## 📋 Prerequisites

1. **Google Cloud Project** with Vision API enabled
2. **Firebase Functions** deployed
3. **Firebase Storage** configured
4. **Admin access** to SupremeAI dashboard

## 🛠️ Setup Instructions

### 1. Install Dependencies

```bash
cd functions
npm install
```

### 2. Deploy Firebase Functions

```bash
firebase deploy --only functions
```

### 3. Access the Feature

- Login to SupremeAI admin dashboard
- Click "🔍 Bengali OCR" in the navigation
- Upload Bengali document images
- Process and export to Excel

## 🔧 API Endpoints

### Process OCR

```
POST /processBengaliOCR
Content-Type: application/json

{
  "imageUrls": ["https://..."],
  "projectId": "optional_project_id",
  "userId": "firebase_user_id"
}
```

### Get OCR Results

```
GET /getOCRResults?projectId=project_id
```

### Export to Excel

```
POST /exportOCRToExcel
Content-Type: application/json

{
  "projectId": "project_id",
  "resultIds": ["result_id_1", "result_id_2"]
}
```

## 📊 Supported Formats

- **Input**: JPG, PNG images with Bengali text
- **Output**: Structured Excel files (.xlsx)
- **Language**: Bengali (with English fallback)

## 💰 Cost Information

- **Google Cloud Vision API**: $1.50 per 1000 images
- **Firebase Storage**: Standard rates apply
- **Firebase Functions**: Pay-per-use

## 🔍 How It Works

1. **Upload Images** via the web interface
2. **Firebase Storage** temporarily stores images
3. **Google Cloud Vision** processes Bengali OCR
4. **Results stored** in Firestore
5. **Excel generation** with structured data
6. **Download links** provided to users

## 🐛 Troubleshooting

### Common Issues

1. **"API not enabled"**: Enable Cloud Vision API in Google Cloud Console
2. **"Permission denied"**: Check service account permissions
3. **"Quota exceeded"**: Monitor usage in Google Cloud Console
4. **"Invalid image"**: Ensure images are valid JPG/PNG under 10MB

### Debug Steps

1. Check Firebase Functions logs: `firebase functions:log`
2. Verify API keys in Google Cloud Console
3. Test with smaller images first
4. Check network connectivity

## 🎯 Usage Examples

### Web Interface

1. Go to `/bengali-ocr.html`
2. Drag & drop Bengali document images
3. Click "Process OCR"
4. View results and export to Excel

### API Integration

```javascript
// Upload and process
const result = await fetch('/processBengaliOCR', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    imageUrls: ['gs://bucket/image.jpg'],
    userId: currentUser.uid
  })
});
```

## 📈 Future Enhancements

- Multi-language support (Hindi, Urdu, etc.)
- Handwriting recognition
- Advanced table parsing
- PDF processing
- Real-time processing status
- Batch export options

## 🤝 Contributing

To add new OCR features:

1. Update `functions/index.js`
2. Modify `public/bengali-ocr.html`
3. Test with various image types
4. Deploy and verify functionality

## 📞 Support

For issues with Bengali OCR:

1. Check the troubleshooting section
2. Review Firebase Functions logs
3. Verify Google Cloud API status
4. Contact the development team

---

**Note**: This feature requires active Google Cloud billing to function properly.
